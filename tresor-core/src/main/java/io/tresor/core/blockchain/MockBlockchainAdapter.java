package io.tresor.core.blockchain;

import io.tresor.core.secretsharing.DeploymentOrchestrator.ChainAdapter;
import io.tresor.core.secretsharing.SecretHolderService.*;
import io.tresor.core.secretsharing.ShamirSecretSharing;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock blockchain adapter for testing.
 *
 * Simulates blockchain deployment without actually hitting real networks.
 * Useful for:
 * - Unit tests
 * - Integration tests
 * - Development environment
 * - Cost-free testing
 *
 * Features:
 * - Configurable success/failure rate
 * - Simulated network latency
 * - In-memory storage of deployed shares
 * - Cost estimation
 *
 * Usage:
 * ```java
 * MockBlockchainAdapter adapter = new MockBlockchainAdapter(Blockchain.BITCOIN);
 * adapter.setSuccessRate(0.8); // 80% success rate
 * adapter.setSimulatedLatencyMs(100); // 100ms delay
 * ```
 */
@Slf4j
public class MockBlockchainAdapter implements ChainAdapter {

    private final Blockchain blockchain;

    // Configuration
    private double successRate = 1.0; // 100% success by default
    private long simulatedLatencyMs = 50; // Simulate 50ms network delay
    private boolean simulateTimeout = false;

    // In-memory storage of deployed shares
    private final Map<String, DeployedShare> deployedShares = new ConcurrentHashMap<>();

    public MockBlockchainAdapter(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public ShareDeployment deploy(
        ShamirSecretSharing.Share share,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) throws Exception {

        log.debug("Mock deploying share {} to {}", share.getX(), blockchain.getName());

        // Simulate network latency
        if (simulatedLatencyMs > 0) {
            Thread.sleep(simulatedLatencyMs);
        }

        // Simulate timeout
        if (simulateTimeout) {
            throw new java.util.concurrent.TimeoutException(
                "Simulated timeout for " + blockchain.getName()
            );
        }

        // Simulate random failures based on success rate
        if (Math.random() > successRate) {
            throw new Exception(
                "Simulated deployment failure for " + blockchain.getName() +
                " (random failure based on success rate)"
            );
        }

        // Generate mock transaction ID
        String transactionId = "mock-tx-" + UUID.randomUUID().toString().substring(0, 8);

        // Calculate unlock block height
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long targetTimestamp = unlockDate.toEpochSecond(ZoneOffset.UTC);
        long currentHeight = getCurrentMockBlockHeight(blockchain);
        long unlockHeight = blockchain.calculateBlockHeight(
            currentHeight,
            currentTimestamp,
            targetTimestamp
        );

        // Store deployed share (for retrieval later)
        DeployedShare deployed = new DeployedShare(
            share,
            unlockDate,
            unlockHeight,
            transactionId
        );
        deployedShares.put(transactionId, deployed);

        // Calculate mock cost
        double cost = estimateCost(blockchain);

        ShareDeployment deployment = ShareDeployment.builder()
            .shareNumber(share.getX())
            .blockchain(blockchain)
            .identifier(transactionId)
            .unlockBlockHeight(unlockHeight)
            .costUsd(cost)
            .status(DeploymentStatus.CONFIRMED)
            .build();

        log.info("✅ Mock deployed share {} to {} (tx: {}, cost: ${}, unlock height: {})",
            share.getX(), blockchain.getName(), transactionId, cost, unlockHeight);

        return deployment;
    }

    @Override
    public boolean supportsRollback() {
        // Mock adapter supports rollback (unlike real blockchains)
        return true;
    }

    @Override
    public void rollback(ShareDeployment deployment) throws Exception {
        log.warn("Mock rolling back deployment: {}", deployment.getIdentifier());

        deployedShares.remove(deployment.getIdentifier());

        log.info("✅ Mock rollback complete for {}", deployment.getIdentifier());
    }

    /**
     * Retrieve a deployed share (for testing unlock flow).
     */
    public ShamirSecretSharing.Share retrieveShare(String transactionId, long currentBlockHeight)
        throws Exception {

        DeployedShare deployed = deployedShares.get(transactionId);

        if (deployed == null) {
            throw new Exception("Share not found: " + transactionId);
        }

        if (currentBlockHeight < deployed.unlockBlockHeight) {
            throw new Exception(
                String.format("Share not yet unlocked: current=%d, unlock=%d",
                    currentBlockHeight, deployed.unlockBlockHeight)
            );
        }

        log.info("✅ Retrieved share from {} (tx: {})", blockchain.getName(), transactionId);

        return deployed.share;
    }

    /**
     * Check if share is unlocked at current block height.
     */
    public boolean isUnlocked(String transactionId, long currentBlockHeight) {
        DeployedShare deployed = deployedShares.get(transactionId);

        if (deployed == null) {
            return false;
        }

        return currentBlockHeight >= deployed.unlockBlockHeight;
    }

    // ========================================
    // CONFIGURATION METHODS
    // ========================================

    public void setSuccessRate(double rate) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("Success rate must be between 0.0 and 1.0");
        }
        this.successRate = rate;
    }

    public void setSimulatedLatencyMs(long latencyMs) {
        if (latencyMs < 0) {
            throw new IllegalArgumentException("Latency must be non-negative");
        }
        this.simulatedLatencyMs = latencyMs;
    }

    public void setSimulateTimeout(boolean timeout) {
        this.simulateTimeout = timeout;
    }

    public int getDeployedShareCount() {
        return deployedShares.size();
    }

    public void clearDeployedShares() {
        deployedShares.clear();
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private long getCurrentMockBlockHeight(Blockchain blockchain) {
        // Return realistic mock block heights
        switch (blockchain) {
            case BITCOIN:
                return 850000; // Approximate current Bitcoin height
            case ETHEREUM:
                return 18500000; // Approximate current Ethereum height
            case ARBITRUM:
                return 150000000; // Arbitrum has higher block numbers
            case POLYGON:
                return 50000000; // Polygon block height
            case CARDANO:
                return 10000000; // Cardano slot number
            case SELF_CUSTODY:
                return 0; // Not applicable
            case ARWEAVE:
                return 1400000; // Arweave block height
            default:
                return 1000000;
        }
    }

    private double estimateCost(Blockchain blockchain) {
        // Mock costs (realistic estimates)
        switch (blockchain) {
            case BITCOIN:
                return 7.0; // Bitcoin OP_RETURN
            case ETHEREUM:
                return 5.0; // Ethereum gas
            case ARBITRUM:
                return 0.50; // Cheap L2
            case POLYGON:
                return 0.01; // Very cheap
            case CARDANO:
                return 1.0; // Cardano metadata
            case SELF_CUSTODY:
                return 0.0; // Free
            case ARWEAVE:
                return 0.50; // Permanent storage
            default:
                return 1.0;
        }
    }

    /**
     * Internal storage class for deployed shares.
     */
    private static class DeployedShare {
        final ShamirSecretSharing.Share share;
        final LocalDateTime unlockDate;
        final long unlockBlockHeight;
        final String transactionId;

        DeployedShare(
            ShamirSecretSharing.Share share,
            LocalDateTime unlockDate,
            long unlockBlockHeight,
            String transactionId
        ) {
            this.share = share;
            this.unlockDate = unlockDate;
            this.unlockBlockHeight = unlockBlockHeight;
            this.transactionId = transactionId;
        }
    }
}
