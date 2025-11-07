package io.tresor.core.storage;

import io.tresor.core.model.EncryptedMessage;

/**
 * Interface for storing encrypted messages.
 * Implementations can use different storage backends:
 * - Arweave (permanent, decentralized)
 * - IPFS (content-addressed, distributed)
 * - S3 (traditional cloud storage)
 * - Local filesystem (for testing)
 *
 * This abstraction allows self-hosters to choose their preferred storage.
 */
public interface StorageService {

    /**
     * Save an encrypted message to storage.
     *
     * @param message The encrypted message to store
     * @return Storage identifier (e.g., Arweave TX ID, IPFS CID, S3 key)
     * @throws StorageException if storage operation fails
     */
    String save(EncryptedMessage message) throws StorageException;

    /**
     * Retrieve an encrypted message from storage.
     *
     * @param identifier The storage identifier returned from save()
     * @return The encrypted message
     * @throws StorageException if retrieval fails or message not found
     */
    EncryptedMessage retrieve(String identifier) throws StorageException;

    /**
     * Check if a message exists in storage.
     *
     * @param identifier The storage identifier
     * @return true if message exists, false otherwise
     */
    boolean exists(String identifier);

    /**
     * Calculate the cost of storing a message.
     *
     * @param sizeBytes Size of the message in bytes
     * @return Estimated cost in USD
     */
    StorageCost calculateCost(long sizeBytes);

    /**
     * Get information about the storage backend.
     *
     * @return Storage provider information
     */
    StorageInfo getInfo();
}
