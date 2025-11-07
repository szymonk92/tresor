package io.tresor.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents an encrypted message with time-lock protection.
 * This is the core data structure that gets stored in Arweave.
 */
@Data
@Builder
public class EncryptedMessage {

    /**
     * Version of the encryption format (for future compatibility)
     */
    private String version;

    /**
     * Encrypted content (text + files)
     */
    private byte[] encryptedPayload;

    /**
     * Time-locked encryption key
     * This key is encrypted using witness encryption or time-lock puzzle
     * and can only be decrypted when the target block height is reached
     */
    private byte[] timeLockedKey;

    /**
     * Target unlock date/time
     */
    private LocalDateTime unlockDate;

    /**
     * Target Bitcoin block height
     */
    private int bitcoinBlockTarget;

    /**
     * Initialization vector for AES encryption
     */
    private byte[] iv;

    /**
     * SHA-256 hash of the plaintext content (for integrity verification)
     */
    private byte[] contentHash;

    /**
     * Metadata (NOT encrypted)
     */
    private MessageMetadata metadata;

    /**
     * When this message was created
     */
    private LocalDateTime createdAt;
}
