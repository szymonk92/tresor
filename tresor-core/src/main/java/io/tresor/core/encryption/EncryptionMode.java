package io.tresor.core.encryption;

/**
 * Encryption modes for messages.
 *
 * Tresor supports multiple encryption modes to balance security,
 * complexity, and cost.
 */
public enum EncryptionMode {

    /**
     * Full encryption with Shamir Secret Sharing.
     *
     * Security: Maximum (cryptographically guaranteed)
     * Complexity: High (key splitting, multi-chain deployment)
     * Cost: $0.57 (budget) to $13 (premium)
     * Use case: Legal documents, wills, high-value data
     *
     * Process:
     * 1. Encrypt message with AES-256-GCM
     * 2. Split encryption key using Shamir (3-of-5 or 5-of-7)
     * 3. Deploy shares to multiple blockchains
     * 4. At unlock: retrieve shares, reconstruct key, decrypt
     */
    FULL_ENCRYPTION,

    /**
     * Password-based encryption.
     *
     * Security: Good (depends on password strength)
     * Complexity: Low (user provides password)
     * Cost: $0.50 (just Arweave storage, no blockchain key management)
     * Use case: Personal messages, time capsules (90% of users)
     *
     * Process:
     * 1. User provides password
     * 2. Derive key using PBKDF2 (100,000 iterations)
     * 3. Encrypt message with AES-256-GCM
     * 4. Store ciphertext on Arweave
     * 5. Store password hint + salt on blockchain
     * 6. At unlock: user enters password, derive key, decrypt
     */
    PASSWORD_ENCRYPTION,

    /**
     * No encryption (time-lock only).
     *
     * Security: Low (message stored in plaintext)
     * Complexity: Minimal
     * Cost: $0.50 (just Arweave storage + blockchain metadata)
     * Use case: Non-sensitive reminders, public announcements
     *
     * Process:
     * 1. Store message in plaintext on Arweave
     * 2. Store unlock_date + arweave_id on blockchain
     * 3. Blockchain prevents access until unlock_date
     * 4. At unlock: retrieve plaintext from Arweave
     */
    NO_ENCRYPTION;

    /**
     * Get default encryption mode (recommended for most users).
     */
    public static EncryptionMode getDefault() {
        return PASSWORD_ENCRYPTION; // Simpler for 90% of users
    }

    /**
     * Check if this mode requires blockchain key management.
     */
    public boolean requiresKeySharing() {
        return this == FULL_ENCRYPTION;
    }

    /**
     * Check if this mode requires a password from the user.
     */
    public boolean requiresPassword() {
        return this == PASSWORD_ENCRYPTION;
    }

    /**
     * Get estimated cost for this encryption mode.
     */
    public double getBaseCost() {
        switch (this) {
            case FULL_ENCRYPTION:
                return 0.57; // Budget tier with Shamir
            case PASSWORD_ENCRYPTION:
                return 0.50; // Just Arweave, no blockchain shares
            case NO_ENCRYPTION:
                return 0.50; // Just Arweave metadata
            default:
                return 0.50;
        }
    }
}
