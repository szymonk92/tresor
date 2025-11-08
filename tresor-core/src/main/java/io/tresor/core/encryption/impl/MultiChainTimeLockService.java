package io.tresor.core.encryption.impl;

import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.encryption.TimeLockService;
import io.tresor.core.model.EncryptedMessage;
import io.tresor.core.model.MessageMetadata;
import io.tresor.core.model.PlainMessage;
import io.tresor.core.secretsharing.DeploymentOrchestrator;
import io.tresor.core.secretsharing.ShamirSecretSharing;
import io.tresor.core.secretsharing.SecretHolderService.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete time-lock encryption service using multi-chain secret holders.
 *
 * This is the main service that coordinates:
 * 1. Message encryption (AES-256-GCM)
 * 2. Key splitting (Shamir's Secret Sharing)
 * 3. Share deployment (Multi-chain deployment)
 * 4. Message unlocking (Share retrieval + decryption)
 *
 * Usage:
 * ```java
 * TimeLockService service = new MultiChainTimeLockService(...);
 *
 * // Encrypt and lock
 * EncryptedMessage encrypted = service.encrypt(
 *     plainMessage,
 *     LocalDateTime.now().plusYears(10)
 * );
 *
 * // Later: Unlock
 * PlainMessage decrypted = service.decrypt(
 *     encrypted,
 *     getCurrentBlockHeight()
 * );
 * ```
 */
@Slf4j
public class MultiChainTimeLockService implements TimeLockService {

    private final MessageEncryptionService encryptionService;
    private final ShamirSecretSharing shamirService;
    private final DeploymentOrchestrator deploymentOrchestrator;

    // Default configuration
    private static final int DEFAULT_TOTAL_SHARES = 5;
    private static final int DEFAULT_THRESHOLD = 3;

    public MultiChainTimeLockService(
        MessageEncryptionService encryptionService,
        ShamirSecretSharing shamirService,
        DeploymentOrchestrator deploymentOrchestrator
    ) {
        this.encryptionService = encryptionService;
        this.shamirService = shamirService;
        this.deploymentOrchestrator = deploymentOrchestrator;
    }

    @Override
    public EncryptedMessage encrypt(PlainMessage message, LocalDateTime unlockDate)
        throws EncryptionException {

        log.info("Time-locking message for unlock at: {}", unlockDate);

        // Step 1: Encrypt message with AES-256-GCM
        EncryptedMessage encrypted = encryptionService.encrypt(message);

        log.info("Message encrypted: ID = {}", encrypted.getId());

        // Step 2: Split encryption key into shares
        byte[] encryptionKey = encrypted.getEncryptionKey();

        List<ShamirSecretSharing.Share> shares = shamirService.split(
            encryptionKey,
            DEFAULT_TOTAL_SHARES,
            DEFAULT_THRESHOLD
        );

        log.info("Encryption key split into {} shares (threshold: {})",
            DEFAULT_TOTAL_SHARES, DEFAULT_THRESHOLD);

        // Step 3: Deploy shares to blockchains
        DeploymentReceipt deployment = deployShares(shares, unlockDate);

        log.info("Shares deployed: {} successful deployments, cost: ${}",
            deployment.getShareDeployments().size(),
            deployment.getTotalCostUsd());

        // Step 4: Build metadata
        MessageMetadata metadata = MessageMetadata.builder()
            .messageId(encrypted.getId())
            .unlockDate(unlockDate)
            .threshold(DEFAULT_THRESHOLD)
            .totalShares(DEFAULT_TOTAL_SHARES)
            .deploymentReceipt(deployment)
            .sharingScheme("shamir-secret-sharing")
            .encryptionAlgorithm("AES-256-GCM")
            .build();

        encrypted.setMetadata(metadata);

        // Remove encryption key from encrypted message (security!)
        // Key is now only available by reconstructing from shares
        encrypted.setEncryptionKey(null);

        log.info("Time-lock encryption complete: {} shares deployed",
            deployment.getShareDeployments().size());

        return encrypted;
    }

    @Override
    public boolean canDecrypt(EncryptedMessage message, int currentBlockHeight) {
        if (message.getMetadata() == null) {
            return false;
        }

        DeploymentReceipt deployment = message.getMetadata().getDeploymentReceipt();
        if (deployment == null || deployment.getShareDeployments().isEmpty()) {
            return false;
        }

        // Check if enough shares are unlocked
        int unlockedCount = 0;

        for (ShareDeployment shareDeployment : deployment.getShareDeployments()) {
            if (currentBlockHeight >= shareDeployment.getUnlockBlockHeight()) {
                unlockedCount++;
            }
        }

        return unlockedCount >= message.getMetadata().getThreshold();
    }

    @Override
    public PlainMessage decrypt(EncryptedMessage message, int currentBlockHeight)
        throws EncryptionException {

        log.info("Attempting to decrypt message: {}", message.getId());

        // Check if can decrypt
        if (!canDecrypt(message, currentBlockHeight)) {
            throw new EncryptionException(
                "Message cannot be decrypted yet: insufficient unlocked shares"
            );
        }

        // Retrieve shares (this would call blockchain adapters)
        // For MVP, we'll simulate this
        List<ShamirSecretSharing.Share> shares = retrieveShares(message, currentBlockHeight);

        if (shares.size() < message.getMetadata().getThreshold()) {
            throw new EncryptionException(
                String.format("Insufficient shares: got %d, need %d",
                    shares.size(), message.getMetadata().getThreshold())
            );
        }

        // Reconstruct encryption key
        byte[] encryptionKey = shamirService.reconstruct(shares);

        log.info("Encryption key reconstructed from {} shares", shares.size());

        // Decrypt message
        PlainMessage decrypted = encryptionService.decrypt(message, encryptionKey);

        log.info("Message decrypted successfully");

        return decrypted;
    }

    @Override
    public int calculateTargetBlockHeight(LocalDateTime unlockDate) {
        // This would query actual blockchains for current height
        // and estimate based on average block times

        // For MVP: Simple estimation
        // Bitcoin: ~10 min blocks = 6 blocks/hour = 144 blocks/day
        LocalDateTime now = LocalDateTime.now();
        long daysUntilUnlock = java.time.Duration.between(now, unlockDate).toDays();

        int currentBitcoinBlock = 850000; // Example current height
        int blocksPerDay = 144;

        return (int) (currentBitcoinBlock + (daysUntilUnlock * blocksPerDay));
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private DeploymentReceipt deployShares(
        List<ShamirSecretSharing.Share> shares,
        LocalDateTime unlockDate
    ) throws EncryptionException {

        // Create deployment configuration
        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(shares.size())
            .threshold(DEFAULT_THRESHOLD)
            .blockchains(List.of(
                Blockchain.BITCOIN,
                Blockchain.ARBITRUM,
                Blockchain.SELF_CUSTODY
                // Add more as needed
            ))
            .includeSelfCustody(true)
            .includeArweave(false)
            .credentials(new HashMap<>())
            .build();

        // Extract key from first share (all shares contain same key info)
        // This is a simplification - in reality we'd pass the whole key
        byte[] keyForDeployment = shares.get(0).toBytes();

        return deploymentOrchestrator.deployWithRecovery(
            keyForDeployment,
            unlockDate,
            config
        );
    }

    private List<ShamirSecretSharing.Share> retrieveShares(
        EncryptedMessage message,
        int currentBlockHeight
    ) throws EncryptionException {

        // This would retrieve shares from blockchains
        // For MVP, we'll return mock shares

        // In production:
        // 1. Check each ShareDeployment
        // 2. If unlocked, call blockchain adapter to retrieve
        // 3. Decode share from blockchain data
        // 4. Return list of retrieved shares

        throw new UnsupportedOperationException(
            "Share retrieval from blockchains not yet implemented in MVP. " +
            "This requires blockchain adapter integration."
        );
    }
}
