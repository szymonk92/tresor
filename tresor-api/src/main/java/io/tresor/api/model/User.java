package io.tresor.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User entity representing a Tresor user.
 *
 * Security considerations:
 * - Email is the primary identifier
 * - No password stored (magic link authentication)
 * - Email verification required
 * - Audit trail maintained
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
    }
)
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * User's email address (primary identifier).
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Whether email has been verified.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * When email was verified.
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /**
     * User's display name (optional).
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * When user account was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When user last logged in.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Account status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Total number of messages created.
     */
    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    /**
     * Total spent on deployments (USD).
     */
    @Column(name = "total_spent_usd", nullable = false)
    private double totalSpentUsd = 0.0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    /**
     * User account status.
     */
    public enum UserStatus {
        ACTIVE,      // Normal active account
        SUSPENDED,   // Temporarily suspended
        DELETED      // Soft deleted
    }
}
