package io.tresor.api.email;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Email service for sending notifications.
 *
 * Supports multiple providers:
 * - SendGrid (production)
 * - AWS SES (production)
 * - Mock (testing)
 */
public interface EmailService {

    /**
     * Send email.
     *
     * @param email Email details
     * @return true if sent successfully
     */
    boolean sendEmail(Email email);

    /**
     * Send welcome email to new user.
     */
    boolean sendWelcomeEmail(String toEmail, String userName);

    /**
     * Send message created confirmation.
     */
    boolean sendMessageCreatedEmail(String toEmail, String messageId, LocalDateTime unlockDate, double cost);

    /**
     * Send message unlocked notification.
     */
    boolean sendMessageUnlockedEmail(String toEmail, String messageId, String messageContent);

    /**
     * Send payment receipt.
     */
    boolean sendPaymentReceipt(String toEmail, String paymentId, double amount, String tier);

    /**
     * Send batch deployment notification.
     */
    boolean sendBatchDeploymentNotification(String toEmail, int messageCount, LocalDateTime deploymentTime);

    /**
     * Email data transfer object.
     */
    @Data
    @Builder
    class Email {
        private String to;
        private String from;
        private String subject;
        private String textBody;
        private String htmlBody;
        private Map<String, String> headers;
        private String templateId;
        private Map<String, Object> templateData;
    }

    /**
     * Email sending result.
     */
    @Data
    @Builder
    class EmailResult {
        private boolean success;
        private String messageId;
        private String error;
        private LocalDateTime sentAt;
    }
}
