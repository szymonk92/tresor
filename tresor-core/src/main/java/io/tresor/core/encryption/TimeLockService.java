package io.tresor.core.encryption;

import io.tresor.core.model.EncryptedMessage;
import io.tresor.core.model.PlainMessage;

import java.time.LocalDateTime;

/**
 * Service for time-locked encryption and decryption.
 * This is the core interface that defines how messages are encrypted
 * with time-lock protection.
 *
 * Implementations:
 * - WitnessEncryptionService: Uses Bitcoin blockchain + witness encryption
 * - TimeLockPuzzleService: Uses time-lock puzzles (sequential computation)
 */
public interface TimeLockService {

    /**
     * Encrypt a message with time-lock protection.
     *
     * @param message The plaintext message to encrypt
     * @param unlockDate The date/time when the message should become decryptable
     * @return Encrypted message with time-locked key
     * @throws EncryptionException if encryption fails
     */
    EncryptedMessage encrypt(PlainMessage message, LocalDateTime unlockDate)
            throws EncryptionException;

    /**
     * Check if a message can be decrypted based on current block height.
     *
     * @param message The encrypted message
     * @param currentBlockHeight Current Bitcoin block height
     * @return true if the message can be decrypted, false otherwise
     */
    boolean canDecrypt(EncryptedMessage message, int currentBlockHeight);

    /**
     * Decrypt a time-locked message.
     * This will only succeed if the target block height has been reached.
     *
     * @param message The encrypted message
     * @param currentBlockHeight Current Bitcoin block height
     * @return Decrypted plaintext message
     * @throws EncryptionException if decryption fails or time-lock not expired
     */
    PlainMessage decrypt(EncryptedMessage message, int currentBlockHeight)
            throws EncryptionException;

    /**
     * Calculate the target Bitcoin block height for a given unlock date.
     *
     * @param unlockDate The desired unlock date
     * @return Estimated Bitcoin block height
     */
    int calculateTargetBlockHeight(LocalDateTime unlockDate);
}
