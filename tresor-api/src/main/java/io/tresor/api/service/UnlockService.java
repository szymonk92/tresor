package io.tresor.api.service;

import io.tresor.api.locking.DistributedLockService;
import io.tresor.api.locking.DistributedLockService.LockException;
import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.secretsharing.ShamirSecretSharing;
import io.tresor.core.secretsharing.ShamirSecretSharing.Share;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for unlocking messages with distributed lock protection.
 *
 * CRITICAL: Prevents race conditions when multiple processes try to unlock same message.
 *
 * Race condition without locks:
 * ```
 * 10:00:00 - Cron job starts unlocking message A
 * 10:00:01 - User clicks "unlock now" → starts unlocking message A
 * 10:00:02 - Both retrieve shares from blockchain
 * 10:00:03 - Both decrypt message
 * 10:00:04 - Both try to save to database → CONFLICT!
 * ```
 *
 * With locks:
 * ```
 * 10:00:00 - Cron job acquires lock → starts unlocking
 * 10:00:01 - User tries to acquire lock → WAITS
 * 10:00:05 - Cron job completes → releases lock
 * 10:00:05 - User acquires lock → sees already unlocked → returns cached result
 * ```
 */
@Slf4j
public class UnlockService {

    private final DistributedLockService lockService;
    private final MessageRepository messageRepository;
    private final ShareRetrievalService shareRetrievalService;
    private final ShamirSecretSharing shamirService;
    private final ArweaveService arweaveService;
    private final EncryptionService encryptionService;

    // Lock configuration
    private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration LOCK_LEASE_TIMEOUT = Duration.ofMinutes(5);

    public UnlockService(
        DistributedLockService lockService,
        MessageRepository messageRepository,
        ShareRetrievalService shareRetrievalService,
        ShamirSecretSharing shamirService,
        ArweaveService arweaveService,
        EncryptionService encryptionService
    ) {
        this.lockService = lockService;
        this.messageRepository = messageRepository;
        this.shareRetrievalService = shareRetrievalService;
        this.shamirService = shamirService;
        this.arweaveService = arweaveService;
        this.encryptionService = encryptionService;
    }

    /**
     * Unlock message with distributed lock protection.
     *
     * Guarantees:
     * - Only one process unlocks a message at a time
     * - Idempotent (safe to call multiple times)
     * - Returns cached result if already unlocked
     *
     * @param messageId Message to unlock
     * @return Unlock result
     * @throws UnlockException if unlock fails
     */
    public UnlockResult unlockMessage(String messageId) throws UnlockException {
        String lockKey = "unlock:" + messageId;

        try {
            // Execute with distributed lock
            return lockService.withLock(
                lockKey,
                LOCK_WAIT_TIMEOUT,
                LOCK_LEASE_TIMEOUT,
                () -> unlockMessageInternal(messageId)
            );

        } catch (LockException.TimeoutException e) {
            // Someone else is unlocking this message
            log.warn("Timeout acquiring lock for {}, checking if already unlocked", messageId);

            // Check if message was unlocked by the other process
            Message message = messageRepository.findById(messageId);
            if (message != null && message.getStatus() == MessageStatus.UNLOCKED) {
                return UnlockResult.alreadyUnlocked(message);
            }

            throw new UnlockException("Another process is unlocking this message", e);

        } catch (LockException e) {
            throw new UnlockException("Failed to acquire lock", e);
        }
    }

    /**
     * Internal unlock logic (runs while holding lock).
     */
    private UnlockResult unlockMessageInternal(String messageId) {
        log.info("Unlocking message: {}", messageId);

        // Step 1: Load message
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new UnlockException("Message not found: " + messageId);
        }

        // Step 2: Check if already unlocked (idempotency)
        if (message.getStatus() == MessageStatus.UNLOCKED) {
            log.info("Message {} already unlocked, returning cached result", messageId);
            return UnlockResult.alreadyUnlocked(message);
        }

        // Step 3: Check if unlock date reached
        if (LocalDateTime.now().isBefore(message.getUnlockDate())) {
            throw new UnlockException("Message not yet ready to unlock");
        }

        // Step 4: Update status to UNLOCKING (prevents concurrent attempts)
        message.setStatus(MessageStatus.UNLOCKING);
        message.setUnlockStartedAt(LocalDateTime.now());
        messageRepository.save(message);

        try {
            // Step 5: Retrieve shares from blockchains
            List<Share> shares = shareRetrievalService.retrieveShares(message);

            if (shares.size() < message.getThreshold()) {
                throw new UnlockException(
                    String.format("Insufficient shares: got %d, need %d",
                        shares.size(), message.getThreshold())
                );
            }

            // Step 6: Reconstruct encryption key
            byte[] encryptionKey = shamirService.reconstruct(shares);

            // Step 7: Download encrypted message from Arweave
            byte[] encryptedPayload = arweaveService.download(message.getArweaveId());

            // Step 8: Decrypt message
            byte[] plaintext = encryptionService.decrypt(encryptedPayload, encryptionKey);

            // Step 9: Re-encrypt with user's personal key for inbox storage
            byte[] userKey = deriveUserKey(message.getUserId());
            byte[] inboxEncrypted = encryptionService.encrypt(plaintext, userKey);

            // Step 10: Store in unlocked_messages table
            UnlockedMessage unlocked = new UnlockedMessage();
            unlocked.setMessageId(messageId);
            unlocked.setUserId(message.getUserId());
            unlocked.setContent(inboxEncrypted);
            unlocked.setUnlockedAt(LocalDateTime.now());
            unlockedMessageRepository.save(unlocked);

            // Step 11: Update message status
            message.setStatus(MessageStatus.UNLOCKED);
            message.setUnlockedAt(LocalDateTime.now());
            messageRepository.save(message);

            log.info("Successfully unlocked message: {}", messageId);

            // Step 12: Send notification
            notificationService.sendUnlockNotification(message);

            return UnlockResult.success(message, plaintext.length);

        } catch (Exception e) {
            // Rollback status on failure
            message.setStatus(MessageStatus.LOCKED);
            message.setUnlockError(e.getMessage());
            messageRepository.save(message);

            log.error("Failed to unlock message {}: {}", messageId, e.getMessage());
            throw new UnlockException("Unlock failed", e);
        }
    }

    /**
     * Batch unlock messages (used by cron job).
     *
     * @param messageIds Messages to unlock
     * @return Results for each message
     */
    public List<UnlockResult> unlockMessages(List<String> messageIds) {
        log.info("Batch unlocking {} messages", messageIds.size());

        return messageIds.stream()
            .map(id -> {
                try {
                    return unlockMessage(id);
                } catch (UnlockException e) {
                    log.error("Failed to unlock {}: {}", id, e.getMessage());
                    return UnlockResult.failure(id, e.getMessage());
                }
            })
            .toList();
    }

    // Helper methods

    private byte[] deriveUserKey(String userId) {
        // Derive user-specific key for re-encryption
        // This would use PBKDF2 with user email + secret pepper
        // Placeholder implementation
        return new byte[32];
    }

    // Placeholder services
    interface MessageRepository {
        Message findById(String id);
        void save(Message message);
    }

    interface ShareRetrievalService {
        List<Share> retrieveShares(Message message);
    }

    interface EncryptionService {
        byte[] decrypt(byte[] ciphertext, byte[] key) throws EncryptionException;
        byte[] encrypt(byte[] plaintext, byte[] key) throws EncryptionException;
    }

    interface ArweaveService {
        byte[] download(String arweaveId);
    }

    interface NotificationService {
        void sendUnlockNotification(Message message);
    }

    interface UnlockedMessageRepository {
        void save(UnlockedMessage unlocked);
    }

    UnlockedMessageRepository unlockedMessageRepository;
    NotificationService notificationService;

    // Value objects

    @Data
    static class Message {
        private String id;
        private String userId;
        private String arweaveId;
        private LocalDateTime unlockDate;
        private int threshold;
        private MessageStatus status;
        private LocalDateTime unlockedAt;
        private LocalDateTime unlockStartedAt;
        private String unlockError;
    }

    @Data
    static class UnlockedMessage {
        private String messageId;
        private String userId;
        private byte[] content;
        private LocalDateTime unlockedAt;
    }

    enum MessageStatus {
        LOCKED,
        UNLOCKING,
        UNLOCKED,
        FAILED
    }

    @Data
    @Builder
    public static class UnlockResult {
        private String messageId;
        private boolean success;
        private String error;
        private long bytesDecrypted;
        private LocalDateTime unlockedAt;
        private boolean wasAlreadyUnlocked;

        public static UnlockResult success(Message message, long bytesDecrypted) {
            return UnlockResult.builder()
                .messageId(message.getId())
                .success(true)
                .bytesDecrypted(bytesDecrypted)
                .unlockedAt(LocalDateTime.now())
                .wasAlreadyUnlocked(false)
                .build();
        }

        public static UnlockResult alreadyUnlocked(Message message) {
            return UnlockResult.builder()
                .messageId(message.getId())
                .success(true)
                .unlockedAt(message.getUnlockedAt())
                .wasAlreadyUnlocked(true)
                .build();
        }

        public static UnlockResult failure(String messageId, String error) {
            return UnlockResult.builder()
                .messageId(messageId)
                .success(false)
                .error(error)
                .build();
        }
    }

    public static class UnlockException extends RuntimeException {
        public UnlockException(String message) {
            super(message);
        }

        public UnlockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
