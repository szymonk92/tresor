package io.tresor.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata about a message (NOT encrypted).
 * This information is stored alongside the encrypted message
 * but can be read without decryption.
 */
@Data
@Builder
public class MessageMetadata {

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

    public enum ContentType {
        TEXT,
        IMAGE,
        AUDIO,
        VIDEO,
        MIXED
    }
}
