package io.tresor.core.storage;

import lombok.Builder;
import lombok.Data;

/**
 * Information about a storage provider.
 */
@Data
@Builder
public class StorageInfo {

    /**
     * Name of the storage provider
     */
    private String provider;

    /**
     * Type of storage (permanent, temporary, cached)
     */
    private StorageType type;

    /**
     * Whether the storage is decentralized
     */
    private boolean decentralized;

    /**
     * Maximum file size supported (in bytes)
     */
    private long maxFileSizeBytes;

    /**
     * Average retrieval time in milliseconds
     */
    private long avgRetrievalTimeMs;

    public enum StorageType {
        PERMANENT,   // Arweave
        REPLICATED,  // IPFS with pinning
        TEMPORARY,   // IPFS without pinning
        CENTRALIZED  // S3, local filesystem
    }
}
