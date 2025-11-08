package io.tresor.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tresor.api.model.Message;
import io.tresor.api.model.Message.MessageStatus;
import io.tresor.api.model.User;
import io.tresor.api.repository.MessageRepository;
import io.tresor.api.repository.UserRepository;
import io.tresor.core.blockchain.MockBlockchainAdapter;
import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.encryption.EncryptionMode;
import io.tresor.core.encryption.impl.MessageEncryptionService;
import io.tresor.core.encryption.impl.MultiChainTimeLockService;
import io.tresor.core.encryption.impl.PasswordEncryptionService;
import io.tresor.core.encryption.impl.PasswordEncryptionService.PasswordEncryptedMessage;
import io.tresor.core.model.EncryptedMessage;
import io.tresor.core.model.PlainMessage;
import io.tresor.core.secretsharing.DeploymentOrchestrator;
import io.tresor.core.secretsharing.SecretHolderService.*;
import io.tresor.core.secretsharing.ShamirSecretSharing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing time-locked messages.
 *
 * Orchestrates the complete message lifecycle:
 * 1. Create message (encrypt + deploy shares)
 * 2. Monitor unlock status
 * 3. Unlock message (retrieve shares + decrypt)
 * 4. Deliver message (send email)
 */
@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MultiChainTimeLockService timeLockService;
    private final PasswordEncryptionService passwordEncryptionService;
    private final ObjectMapper objectMapper;

    public MessageService(
        MessageRepository messageRepository,
        UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;

        // Initialize encryption services
        MessageEncryptionService encryptionService = new MessageEncryptionService();
        this.passwordEncryptionService = new PasswordEncryptionService();
        ShamirSecretSharing shamirService = new ShamirSecretSharing();

        // For MVP: Use mock blockchain adapters for all tiers
        Map<Blockchain, DeploymentOrchestrator.ChainAdapter> adapters = new HashMap<>();
        adapters.put(Blockchain.BITCOIN, new MockBlockchainAdapter(Blockchain.BITCOIN));
        adapters.put(Blockchain.ETHEREUM, new MockBlockchainAdapter(Blockchain.ETHEREUM));
        adapters.put(Blockchain.ARBITRUM, new MockBlockchainAdapter(Blockchain.ARBITRUM));
        adapters.put(Blockchain.POLYGON, new MockBlockchainAdapter(Blockchain.POLYGON));
        adapters.put(Blockchain.BASE, new MockBlockchainAdapter(Blockchain.BASE));
        adapters.put(Blockchain.OPTIMISM, new MockBlockchainAdapter(Blockchain.OPTIMISM));
        adapters.put(Blockchain.AVALANCHE, new MockBlockchainAdapter(Blockchain.AVALANCHE));

        DeploymentOrchestrator orchestrator = new DeploymentOrchestrator(adapters);

        this.timeLockService = new MultiChainTimeLockService(
            encryptionService,
            shamirService,
            orchestrator
        );

        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For LocalDateTime support
    }

    /**
     * Create a new time-locked message with encryption mode and tier selection.
     *
     * @param userId User creating the message
     * @param textContent Message text
     * @param unlockDate When to unlock
     * @param encryptionMode Encryption mode (FULL_ENCRYPTION, PASSWORD_ENCRYPTION, NO_ENCRYPTION)
     * @param password Password (required for PASSWORD_ENCRYPTION mode)
     * @param passwordHint Optional password hint
     * @param deploymentTier Deployment tier (budget, standard, premium, enterprise)
     * @return Created message
     */
    @Transactional
    public Message createMessage(
        String userId,
        String textContent,
        LocalDateTime unlockDate,
        String encryptionMode,
        String password,
        String passwordHint,
        String deploymentTier
    ) throws EncryptionException {

        log.info("Creating message for user {}, unlock: {}, mode: {}, tier: {}",
            userId, unlockDate, encryptionMode, deploymentTier);

        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate unlock date
        if (unlockDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Unlock date must be in the future");
        }

        // Default encryption mode to PASSWORD_ENCRYPTION
        if (encryptionMode == null || encryptionMode.isEmpty()) {
            encryptionMode = "PASSWORD_ENCRYPTION";
        }

        // Default tier to budget
        if (deploymentTier == null || deploymentTier.isEmpty()) {
            deploymentTier = "budget";
        }

        // Validate password for PASSWORD_ENCRYPTION mode
        if ("PASSWORD_ENCRYPTION".equals(encryptionMode)) {
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password required for PASSWORD_ENCRYPTION mode");
            }
            if (password.length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
        }

        // Create message based on encryption mode
        if ("PASSWORD_ENCRYPTION".equals(encryptionMode)) {
            return createPasswordEncryptedMessage(
                user, textContent, unlockDate, password, passwordHint, deploymentTier
            );
        } else if ("FULL_ENCRYPTION".equals(encryptionMode)) {
            return createFullEncryptedMessage(
                user, textContent, unlockDate, deploymentTier
            );
        } else if ("NO_ENCRYPTION".equals(encryptionMode)) {
            return createNoEncryptionMessage(
                user, textContent, unlockDate, deploymentTier
            );
        } else {
            throw new IllegalArgumentException("Invalid encryption mode: " + encryptionMode);
        }
    }

    /**
     * Create message with password encryption (simple mode).
     */
    private Message createPasswordEncryptedMessage(
        User user,
        String textContent,
        LocalDateTime unlockDate,
        String password,
        String passwordHint,
        String deploymentTier
    ) throws EncryptionException {

        // Create plain message
        PlainMessage plainMessage = PlainMessage.builder()
            .textContent(textContent)
            .createdAt(LocalDateTime.now())
            .deliveryEmail(user.getEmail())
            .files(List.of())
            .build();

        // Encrypt with password
        PasswordEncryptedMessage encrypted = passwordEncryptionService.encrypt(
            plainMessage,
            password,
            passwordHint
        );

        // Create database record
        Message message = new Message();
        message.setUserId(user.getId());
        message.setDeliveryEmail(user.getEmail());
        message.setUnlockDate(unlockDate);

        // Budget tier uses batch deployment (PENDING_BATCH), others deploy immediately (LOCKED)
        message.setStatus(isBudgetTier(deploymentTier) ? MessageStatus.PENDING_BATCH : MessageStatus.LOCKED);

        message.setEncryptionMode("PASSWORD_ENCRYPTION");
        message.setDeploymentTier(deploymentTier);
        message.setPasswordSalt(java.util.Base64.getEncoder().encodeToString(encrypted.getSalt()));
        message.setPasswordHint(passwordHint);
        message.setPbkdf2Iterations(encrypted.getIterations());
        message.setEncryptionIv(java.util.Base64.getEncoder().encodeToString(encrypted.getIv()));
        message.setContentHash(bytesToHex(encrypted.getContentHash()));

        // Cost for password encryption (no blockchain shares needed)
        double cost = getTierBaseCost(deploymentTier);
        message.setCostUsd(cost);

        // TODO: Store encrypted payload in Arweave
        message.setArweaveId("mock-arweave-" + encrypted.getId());

        // Save to database
        message = messageRepository.save(message);

        // Update user statistics
        user.setMessageCount(user.getMessageCount() + 1);
        user.setTotalSpentUsd(user.getTotalSpentUsd() + cost);
        userRepository.save(user);

        log.info("✅ Password-encrypted message created: {} (cost: ${})", message.getId(), cost);

        return message;
    }

    /**
     * Create message with full encryption (Shamir Secret Sharing).
     */
    private Message createFullEncryptedMessage(
        User user,
        String textContent,
        LocalDateTime unlockDate,
        String deploymentTier
    ) throws EncryptionException {

        // This uses the existing MultiChainTimeLockService
        // (same as the old createMessage method)

        PlainMessage plainMessage = PlainMessage.builder()
            .textContent(textContent)
            .createdAt(LocalDateTime.now())
            .deliveryEmail(user.getEmail())
            .files(List.of())
            .build();

        EncryptedMessage encrypted = timeLockService.encrypt(plainMessage, unlockDate);
        double cost = encrypted.getMetadata().getDeploymentReceipt().getTotalCostUsd();

        Message message = new Message();
        message.setUserId(user.getId());
        message.setDeliveryEmail(user.getEmail());
        message.setUnlockDate(unlockDate);

        // Budget tier uses batch deployment (PENDING_BATCH), others deploy immediately (LOCKED)
        message.setStatus(isBudgetTier(deploymentTier) ? MessageStatus.PENDING_BATCH : MessageStatus.LOCKED);

        message.setEncryptionMode("FULL_ENCRYPTION");
        message.setDeploymentTier(deploymentTier);
        message.setContentHash(bytesToHex(encrypted.getContentHash()));
        message.setCostUsd(cost);

        try {
            String receiptJson = objectMapper.writeValueAsString(
                encrypted.getMetadata().getDeploymentReceipt()
            );
            message.setDeploymentReceiptJson(receiptJson);
        } catch (Exception e) {
            throw new EncryptionException("Failed to serialize deployment receipt", e);
        }

        message.setArweaveId("mock-arweave-" + encrypted.getId());
        message = messageRepository.save(message);

        user.setMessageCount(user.getMessageCount() + 1);
        user.setTotalSpentUsd(user.getTotalSpentUsd() + cost);
        userRepository.save(user);

        log.info("✅ Full-encrypted message created: {} (cost: ${})", message.getId(), cost);

        return message;
    }

    /**
     * Create message with no encryption (time-lock only).
     */
    private Message createNoEncryptionMessage(
        User user,
        String textContent,
        LocalDateTime unlockDate,
        String deploymentTier
    ) throws EncryptionException {

        Message message = new Message();
        message.setUserId(user.getId());
        message.setDeliveryEmail(user.getEmail());
        message.setUnlockDate(unlockDate);

        // Budget tier uses batch deployment (PENDING_BATCH), others deploy immediately (LOCKED)
        message.setStatus(isBudgetTier(deploymentTier) ? MessageStatus.PENDING_BATCH : MessageStatus.LOCKED);

        message.setEncryptionMode("NO_ENCRYPTION");
        message.setDeploymentTier(deploymentTier);

        double cost = getTierBaseCost(deploymentTier);
        message.setCostUsd(cost);

        // TODO: Store plaintext in Arweave with access control
        message.setArweaveId("mock-arweave-plaintext-" + java.util.UUID.randomUUID());

        message = messageRepository.save(message);

        user.setMessageCount(user.getMessageCount() + 1);
        user.setTotalSpentUsd(user.getTotalSpentUsd() + cost);
        userRepository.save(user);

        log.info("✅ No-encryption message created: {} (cost: ${})", message.getId(), cost);

        return message;
    }

    /**
     * Get base cost for a deployment tier.
     */
    private double getTierBaseCost(String tier) {
        switch (tier) {
            case "budget":
                return 0.57;
            case "standard":
                return 3.00;
            case "premium":
                return 13.00;
            case "enterprise":
                return 25.00;
            default:
                return 0.57; // Default to budget
        }
    }

    /**
     * Create a new time-locked message (legacy method - uses defaults).
     *
     * @param userId User creating the message
     * @param textContent Message text
     * @param unlockDate When to unlock
     * @return Created message
     */
    @Transactional
    public Message createMessage(
        String userId,
        String textContent,
        LocalDateTime unlockDate
    ) throws EncryptionException {

        // Call new method with defaults
        return createMessage(
            userId,
            textContent,
            unlockDate,
            "PASSWORD_ENCRYPTION",  // Default mode
            "default-password-" + System.currentTimeMillis(), // Auto-generated
            "Auto-generated password",
            "budget"  // Default tier
        );
    }

    /**
     * Original implementation (kept for reference).
     */
    @Deprecated
    private Message createMessageOld(
        String userId,
        String textContent,
        LocalDateTime unlockDate
    ) throws EncryptionException {

        log.info("Creating message for user {}, unlock: {}", userId, unlockDate);

        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate unlock date
        if (unlockDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Unlock date must be in the future");
        }

        // Create plain message
        PlainMessage plainMessage = PlainMessage.builder()
            .textContent(textContent)
            .createdAt(LocalDateTime.now())
            .deliveryEmail(user.getEmail())
            .files(List.of()) // No files for MVP
            .build();

        // Encrypt and deploy
        EncryptedMessage encrypted = timeLockService.encrypt(plainMessage, unlockDate);

        // Calculate cost from deployment receipt
        double cost = encrypted.getMetadata().getDeploymentReceipt().getTotalCostUsd();

        // Create database record
        Message message = new Message();
        message.setUserId(userId);
        message.setDeliveryEmail(user.getEmail());
        message.setUnlockDate(unlockDate);
        message.setStatus(MessageStatus.LOCKED);
        message.setContentHash(bytesToHex(encrypted.getContentHash()));
        message.setCostUsd(cost);

        try {
            // Serialize deployment receipt
            String receiptJson = objectMapper.writeValueAsString(
                encrypted.getMetadata().getDeploymentReceipt()
            );
            message.setDeploymentReceiptJson(receiptJson);
        } catch (Exception e) {
            throw new EncryptionException("Failed to serialize deployment receipt", e);
        }

        // TODO: Store encrypted message in Arweave
        // For MVP: Just store a placeholder
        message.setArweaveId("mock-arweave-" + encrypted.getId());

        // Save to database
        message = messageRepository.save(message);

        // Update user statistics
        user.setMessageCount(user.getMessageCount() + 1);
        user.setTotalSpentUsd(user.getTotalSpentUsd() + cost);
        userRepository.save(user);

        log.info("✅ Message created: {} (cost: ${})", message.getId(), cost);

        return message;
    }

    /**
     * Get all messages for a user.
     */
    public List<Message> getUserMessages(String userId) {
        return messageRepository.findByUserId(userId);
    }

    /**
     * Get messages by status.
     */
    public List<Message> getUserMessagesByStatus(String userId, MessageStatus status) {
        return messageRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Get message by ID (with authorization check).
     */
    public Message getMessage(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to access this message");
        }

        return message;
    }

    /**
     * Get statistics for user.
     */
    public MessageStats getStats(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        long totalMessages = messageRepository.countByUserId(userId);
        long lockedMessages = messageRepository.countByUserIdAndStatus(userId, MessageStatus.LOCKED);
        long unlockedMessages = messageRepository.countByUserIdAndStatus(userId, MessageStatus.UNLOCKED);
        long deliveredMessages = messageRepository.countByUserIdAndStatus(userId, MessageStatus.DELIVERED);

        return new MessageStats(
            totalMessages,
            lockedMessages,
            unlockedMessages,
            deliveredMessages,
            user.getTotalSpentUsd()
        );
    }

    /**
     * Check if a tier uses batch deployment.
     */
    private boolean isBudgetTier(String tier) {
        return "budget".equalsIgnoreCase(tier);
    }

    /**
     * Helper to convert bytes to hex.
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Message statistics.
     */
    public record MessageStats(
        long totalMessages,
        long lockedMessages,
        long unlockedMessages,
        long deliveredMessages,
        double totalSpentUsd
    ) {}
}
