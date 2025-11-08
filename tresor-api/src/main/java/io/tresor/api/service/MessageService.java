package io.tresor.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tresor.api.model.Message;
import io.tresor.api.model.Message.MessageStatus;
import io.tresor.api.model.User;
import io.tresor.api.repository.MessageRepository;
import io.tresor.api.repository.UserRepository;
import io.tresor.core.blockchain.MockBlockchainAdapter;
import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.encryption.impl.MessageEncryptionService;
import io.tresor.core.encryption.impl.MultiChainTimeLockService;
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
    private final ObjectMapper objectMapper;

    public MessageService(
        MessageRepository messageRepository,
        UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;

        // Initialize services
        MessageEncryptionService encryptionService = new MessageEncryptionService();
        ShamirSecretSharing shamirService = new ShamirSecretSharing();

        // For MVP: Use mock blockchain adapters
        Map<Blockchain, DeploymentOrchestrator.ChainAdapter> adapters = new HashMap<>();
        adapters.put(Blockchain.BITCOIN, new MockBlockchainAdapter(Blockchain.BITCOIN));
        adapters.put(Blockchain.ARBITRUM, new MockBlockchainAdapter(Blockchain.ARBITRUM));
        adapters.put(Blockchain.ETHEREUM, new MockBlockchainAdapter(Blockchain.ETHEREUM));

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
     * Create a new time-locked message.
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
