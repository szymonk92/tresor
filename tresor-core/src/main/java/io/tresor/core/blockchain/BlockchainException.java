package io.tresor.core.blockchain;

/**
 * Exception thrown when blockchain operations fail.
 */
public class BlockchainException extends Exception {

    public BlockchainException(String message) {
        super(message);
    }

    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
    }
}
