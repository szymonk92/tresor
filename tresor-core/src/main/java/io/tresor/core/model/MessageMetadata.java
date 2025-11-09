package io.tresor.core.model;

import io.tresor.core.secretsharing.SecretHolderService.DeploymentReceipt;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Metadata about a message (NOT encrypted).
 * This information is stored alongside the encrypted message
 * but can be read without decryption.
 */
@Data
@Builder
public class MessageMetadata {

    /**
     * Unique message identifier
     */
    private String messageId;

    /**
     * User-provided title (optional)
     */
    private String title;

    /**
     * Total size in bytes (encrypted payload + time-locked key)
     */
    private long sizeBytes;

    /**
     * Number of files attached
     */
    private int fileCount;

    /**
     * Type of content (text, image, audio, video)
     */
    private ContentType primaryContentType;

    /**
     * Tags for categorization (optional)
     */
    private String[] tags;

    /**
     * Deployment receipt (for multi-chain deployments)
     */
    private DeploymentReceipt deploymentReceipt;

    /**
     * Shamir secret sharing threshold (e.g., 3 for 3-of-5)
     */
    private int threshold;

    /**
     * Total number of shares created
     */
    private int totalShares;

    /**
     * Target unlock date/time
     */
    private LocalDateTime unlockDate;

    /**
     * Secret sharing scheme used (e.g., "shamir-secret-sharing")
     */
    private String sharingScheme;

    /**
     * Encryption algorithm used (e.g., "AES-256-GCM")
     */
    private String encryptionAlgorithm;

    public enum ContentType {
        TEXT,
        IMAGE,
        AUDIO,
        VIDEO,
        MIXED
    }
}
