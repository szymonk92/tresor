package io.tresor.core.storage;

/**
 * Exception thrown when storage operations fail.
 */
public class StorageException extends Exception {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when a message is not found in storage.
     */
    public static class MessageNotFoundException extends StorageException {
        public MessageNotFoundException(String identifier) {
            super("Message not found: " + identifier);
        }
    }

    /**
     * Exception thrown when storage quota is exceeded.
     */
    public static class StorageQuotaExceededException extends StorageException {
        public StorageQuotaExceededException(String message) {
            super(message);
        }
    }
}
