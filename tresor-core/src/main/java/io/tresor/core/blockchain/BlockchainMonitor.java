package io.tresor.core.blockchain;

/**
 * Interface for monitoring the Bitcoin blockchain.
 * Implementations can use different methods:
 * - Self-hosted Bitcoin node (bitcoinj)
 * - Third-party APIs (BlockCypher, Blockchain.com)
 * - Multiple providers with fallback
 */
public interface BlockchainMonitor {

    /**
     * Get the current Bitcoin block height.
     *
     * @return Current block height
     * @throws BlockchainException if unable to fetch block height
     */
    int getCurrentBlockHeight() throws BlockchainException;

    /**
     * Calculate estimated block height for a future date.
     * Bitcoin produces blocks approximately every 10 minutes.
     *
     * @param futureTimestamp Unix timestamp in seconds
     * @return Estimated block height
     */
    int estimateBlockHeight(long futureTimestamp);

    /**
     * Start monitoring the blockchain.
     * This typically runs in a background thread.
     */
    void startMonitoring();

    /**
     * Stop monitoring the blockchain.
     */
    void stopMonitoring();

    /**
     * Check if monitoring is currently active.
     *
     * @return true if monitoring, false otherwise
     */
    boolean isMonitoring();
}
