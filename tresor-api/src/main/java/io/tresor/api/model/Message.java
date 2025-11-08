package io.tresor.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Message entity representing a time-locked message.
 *
 * Lifecycle:
 * 1. DRAFT - User is composing (not yet paid/deployed)
 * 2. DEPLOYING - Payment captured, shares being deployed
 * 3. LOCKED - Shares deployed successfully, waiting for unlock date
 * 4. UNLOCKING - Unlock date reached, retrieving shares
 * 5. UNLOCKED - Shares retrieved, key reconstructed, message decrypted
 * 6. DELIVERED - Message sent to user's email
 * 7. FAILED - Deployment or unlock failed
 */
@Entity
@Table(name = "messages",
    indexes = {
        @Index(name = "idx_message_user", columnList = "user_id"),
        @Index(name = "idx_message_status", columnList = "status"),
        @Index(name = "idx_message_unlock_date", columnList = "unlock_date")
    }
)
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * User who created this message.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Email address where decrypted message will be delivered.
     */
    @Column(name = "delivery_email", nullable = false, length = 255)
    private String deliveryEmail;

    /**
     * When message was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When message should be unlocked.
     */
    @Column(name = "unlock_date", nullable = false)
    private LocalDateTime unlockDate;

    /**
     * Current status of message.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status = MessageStatus.DRAFT;

    /**
     * Arweave transaction ID where encrypted message is stored.
     */
    @Column(name = "arweave_id", length = 43)
    private String arweaveId;

    /**
     * Deployment receipt (JSON).
     * Contains share deployment details, costs, blockchain identifiers.
     */
    @Column(name = "deployment_receipt", columnDefinition = "TEXT")
    private String deploymentReceiptJson;

    /**
     * SHA-256 hash of plain message content (for verification).
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * Total cost of deployment (USD).
     */
    @Column(name = "cost_usd", nullable = false)
    private double costUsd = 0.0;

    /**
     * When message was unlocked (if unlocked).
     */
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    /**
     * When message was delivered to email (if delivered).
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Next scheduled unlock check (for retry logic).
     */
    @Column(name = "next_unlock_check")
    private LocalDateTime nextUnlockCheck;

    /**
     * Number of unlock attempts made.
     */
    @Column(name = "unlock_attempts", nullable = false)
    private int unlockAttempts = 0;

    /**
     * Error message if deployment or unlock failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = MessageStatus.DRAFT;
        }
    }

    /**
     * Check if message is ready to be unlocked.
     */
    public boolean isReadyToUnlock() {
        return status == MessageStatus.LOCKED &&
               LocalDateTime.now().isAfter(unlockDate);
    }

    /**
     * Message lifecycle status.
     */
    public enum MessageStatus {
        DRAFT,       // User is composing
        DEPLOYING,   // Shares being deployed
        LOCKED,      // Deployed and waiting
        UNLOCKING,   // Retrieving shares
        UNLOCKED,    // Decrypted successfully
        DELIVERED,   // Sent to email
        FAILED       // Deployment or unlock failed
    }
}
