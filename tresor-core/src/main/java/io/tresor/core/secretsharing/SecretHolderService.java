package io.tresor.core.secretsharing;

import io.tresor.core.encryption.EncryptionException;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for deploying secret shares to distributed holders (blockchains, smart contracts).
 *
 * This service coordinates the deployment of Shamir secret shares across
 * multiple independent storage systems to enable decentralized time-lock encryption.
 *
 * Supported holders:
 * - Bitcoin (CLTV time-locks)
 * - Ethereum (smart contracts)
 * - Arbitrum L2 (smart contracts)
 * - Polygon (smart contracts)
 * - Self-custody (encrypted local storage)
 * - Arweave (permanent storage)
 */
public interface SecretHolderService {

    /**
     * Split key and deploy shares to configured holders.
     *
     * @param key The encryption key to split
     * @param unlockDate When the shares should become available
     * @param config Configuration for blockchain deployments
     * @return Receipt with deployment details
     * @throws EncryptionException if deployment fails
     */
    DeploymentReceipt deployShares(
        byte[] key,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) throws EncryptionException;

    /**
     * Retrieve available shares from holders.
     *
     * @param deploymentId Unique identifier from deployment receipt
     * @return List of retrieved shares (may be less than total if some not yet unlocked)
     * @throws EncryptionException if retrieval fails
     */
    List<ShamirSecretSharing.Share> retrieveShares(String deploymentId) throws EncryptionException;

    /**
     * Check which shares are currently available for retrieval.
     *
     * @param deploymentId Unique identifier from deployment receipt
     * @return Status of each share holder
     */
    Map<Blockchain, ShareStatus> checkShareStatus(String deploymentId) throws EncryptionException;

    /**
     * Reconstruct encryption key from retrieved shares.
     *
     * @param shares The shares retrieved from holders
     * @return The reconstructed encryption key
     * @throws EncryptionException if insufficient shares or reconstruction fails
     */
    byte[] reconstructKey(List<ShamirSecretSharing.Share> shares) throws EncryptionException;

    /**
     * Configuration for deploying shares across multiple holders.
     */
    @Data
    @Builder
    public static class DeploymentConfig {
        /**
         * Total number of shares to create
         */
        private int totalShares;

        /**
         * Threshold (minimum shares needed to reconstruct)
         */
        private int threshold;

        /**
         * Which blockchains to deploy to
         */
        private List<Blockchain> blockchains;

        /**
         * Whether to include self-custody share
         */
        private boolean includeSelfCustody;

        /**
         * Whether to include Arweave permanent storage
         */
        private boolean includeArweave;

        /**
         * API keys and credentials for blockchain access
         */
        private Map<String, String> credentials;
    }

    /**
     * Receipt containing deployment details.
     */
    @Data
    @Builder
    public static class DeploymentReceipt {
        /**
         * Unique identifier for this deployment
         */
        private String deploymentId;

        /**
         * When shares were deployed
         */
        private LocalDateTime deployedAt;

        /**
         * When shares will unlock
         */
        private LocalDateTime unlockDate;

        /**
         * Total shares created
         */
        private int totalShares;

        /**
         * Threshold needed to reconstruct
         */
        private int threshold;

        /**
         * Details of each share deployment
         */
        private List<ShareDeployment> shareDeployments;

        /**
         * Self-custody share (if included)
         */
        private byte[] selfCustodyShare;

        /**
         * Total cost of deployment (USD)
         */
        private double totalCostUsd;
    }

    /**
     * Details of a single share deployment.
     */
    @Data
    @Builder
    public static class ShareDeployment {
        /**
         * Share number (1, 2, 3, ...)
         */
        private int shareNumber;

        /**
         * Blockchain where share is stored
         */
        private Blockchain blockchain;

        /**
         * Transaction ID or contract address
         */
        private String identifier;

        /**
         * Target block height for unlock
         */
        private long unlockBlockHeight;

        /**
         * Cost of this deployment (USD)
         */
        private double costUsd;

        /**
         * Deployment status
         */
        private DeploymentStatus status;
    }

    /**
     * Status of a share on a holder.
     */
    @Data
    @Builder
    public static class ShareStatus {
        /**
         * Blockchain holder
         */
        private Blockchain blockchain;

        /**
         * Whether share is available for retrieval
         */
        private boolean unlocked;

        /**
         * Current block height
         */
        private long currentBlockHeight;

        /**
         * Target unlock block height
         */
        private long unlockBlockHeight;

        /**
         * Estimated time until unlock (if not yet unlocked)
         */
        private LocalDateTime estimatedUnlockTime;
    }

    /**
     * Supported blockchains for secret holding.
     */
    enum Blockchain {
        BITCOIN("Bitcoin", 10 * 60), // ~10 min blocks
        ETHEREUM("Ethereum", 12),     // ~12 sec blocks
        ARBITRUM("Arbitrum", 1),      // ~1 sec blocks
        POLYGON("Polygon", 2),        // ~2 sec blocks
        CARDANO("Cardano", 20),       // ~20 sec blocks
        SELF_CUSTODY("Self-custody", 0),
        ARWEAVE("Arweave", 2 * 60);   // ~2 min blocks

        private final String name;
        private final int avgBlockTimeSeconds;

        Blockchain(String name, int avgBlockTimeSeconds) {
            this.name = name;
            this.avgBlockTimeSeconds = avgBlockTimeSeconds;
        }

        public String getName() {
            return name;
        }

        public int getAvgBlockTimeSeconds() {
            return avgBlockTimeSeconds;
        }

        /**
         * Calculate block height for future timestamp.
         */
        public long calculateBlockHeight(long currentHeight, long currentTimestamp, long targetTimestamp) {
            if (avgBlockTimeSeconds == 0) {
                return 0; // Self-custody doesn't use block heights
            }

            long secondsUntilTarget = targetTimestamp - currentTimestamp;
            long blocksUntilTarget = secondsUntilTarget / avgBlockTimeSeconds;

            return currentHeight + blocksUntilTarget;
        }
    }

    /**
     * Deployment status.
     */
    enum DeploymentStatus {
        PENDING,      // Transaction submitted
        CONFIRMED,    // Transaction confirmed on blockchain
        FAILED,       // Deployment failed
        CANCELLED     // Deployment cancelled
    }
}
