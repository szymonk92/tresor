package io.tresor.core.encryption;

/**
 * Exception thrown when encryption or decryption operations fail.
 */
public class EncryptionException extends Exception {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when trying to decrypt a message before its unlock time.
     */
    public static class MessageStillLockedException extends EncryptionException {
        public MessageStillLockedException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when message integrity check fails.
     */
    public static class IntegrityCheckFailedException extends EncryptionException {
        public IntegrityCheckFailedException(String message) {
            super(message);
        }
    }
}
