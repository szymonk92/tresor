package io.tresor.api.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mock email service for testing and development.
 *
 * In production, replace with SendGridEmailService or AwsSesEmailService.
 *
 * This implementation:
 * - Logs emails instead of sending them
 * - Stores sent emails in memory for testing
 * - Simulates successful delivery
 * - Provides email templates
 */
@Slf4j
@Service
public class MockEmailService implements EmailService {

    private final List<Email> sentEmails = new ArrayList<>();
    private static final String FROM_EMAIL = "noreply@tresor.io";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    @Override
    public boolean sendEmail(Email email) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║                  MOCK EMAIL SENT                         ║");
        log.info("╚══════════════════════════════════════════════════════════╝");
        log.info("To: {}", email.getTo());
        log.info("From: {}", email.getFrom());
        log.info("Subject: {}", email.getSubject());
        log.info("─────────────────────────────────────────────────────────");
        log.info("Body:\n{}", email.getTextBody() != null ? email.getTextBody() : email.getHtmlBody());
        log.info("══════════════════════════════════════════════════════════");

        // Store for testing
        sentEmails.add(email);

        return true;
    }

    @Override
    public boolean sendWelcomeEmail(String toEmail, String userName) {
        String subject = "Welcome to Tresor! 🔒";

        String textBody = String.format("""
            Hi %s,

            Welcome to Tresor - your messages to the future, secured by blockchain!

            With Tresor, you can:
            ✉️  Send time-locked messages to your future self
            🔒 Encrypted with military-grade AES-256-GCM
            ⛓️  Secured by Bitcoin and Ethereum blockchains
            💰 Starting at just $0.50 per message

            Ready to create your first message?
            Visit: https://tresor.io/create

            Need help? Reply to this email or visit our documentation:
            https://docs.tresor.io

            Best regards,
            The Tresor Team

            ---
            Tresor - Because some messages are worth waiting for
            """, userName);

        Email email = Email.builder()
            .to(toEmail)
            .from(FROM_EMAIL)
            .subject(subject)
            .textBody(textBody)
            .build();

        return sendEmail(email);
    }

    @Override
    public boolean sendMessageCreatedEmail(String toEmail, String messageId, LocalDateTime unlockDate, double cost) {
        String subject = "Message Created Successfully ✅";

        String formattedDate = unlockDate.format(FORMATTER);

        String textBody = String.format("""
            Your time-locked message has been created successfully!

            Message Details:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Message ID: %s
            Unlock Date: %s
            Cost: $%.2f

            What happens next?
            1. Your message is encrypted with AES-256-GCM
            2. Encryption keys are split and deployed to blockchains
            3. On the unlock date, you'll receive your message via email
            4. You can track status at: https://tresor.io/messages/%s

            Your message is now secured and will remain locked until the specified date.
            Even we cannot access it before then!

            Budget tier messages are deployed in daily batches at midnight for cost
            optimization. Your message will be fully deployed within 24 hours.

            Track your message:
            https://tresor.io/messages/%s

            Questions? support@tresor.io

            Best regards,
            The Tresor Team
            """, messageId, formattedDate, cost, messageId, messageId);

        Email email = Email.builder()
            .to(toEmail)
            .from(FROM_EMAIL)
            .subject(subject)
            .textBody(textBody)
            .build();

        return sendEmail(email);
    }

    @Override
    public boolean sendMessageUnlockedEmail(String toEmail, String messageId, String messageContent) {
        String subject = "Your Message Has Been Unlocked! 🎉";

        String textBody = String.format("""
            The time has come! Your message has been unlocked.

            Message ID: %s

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Your Message:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            %s

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            This message was cryptographically secured and time-locked using:
            • AES-256-GCM encryption
            • Shamir Secret Sharing
            • Multi-chain blockchain deployment
            • Arweave permanent storage

            View full details: https://tresor.io/messages/%s

            Want to send another message to your future self?
            https://tresor.io/create

            Best regards,
            The Tresor Team

            ---
            Tresor - Your message from the past, delivered securely
            """, messageId, messageContent, messageId);

        Email email = Email.builder()
            .to(toEmail)
            .from(FROM_EMAIL)
            .subject(subject)
            .textBody(textBody)
            .build();

        return sendEmail(email);
    }

    @Override
    public boolean sendPaymentReceipt(String toEmail, String paymentId, double amount, String tier) {
        String subject = String.format("Payment Receipt - $%.2f", amount);

        String textBody = String.format("""
            Thank you for your payment!

            Payment Receipt
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Payment ID: %s
            Amount: $%.2f
            Deployment Tier: %s
            Date: %s

            Your payment has been processed successfully and your message is now
            being encrypted and deployed to the blockchain.

            Tier Details:
            %s

            View your invoice: https://tresor.io/payments/%s

            Questions about your payment? support@tresor.io

            Best regards,
            The Tresor Team
            """,
            paymentId,
            amount,
            tier.toUpperCase(),
            LocalDateTime.now().format(FORMATTER),
            getTierDescription(tier),
            paymentId);

        Email email = Email.builder()
            .to(toEmail)
            .from(FROM_EMAIL)
            .subject(subject)
            .textBody(textBody)
            .build();

        return sendEmail(email);
    }

    @Override
    public boolean sendBatchDeploymentNotification(String toEmail, int messageCount, LocalDateTime deploymentTime) {
        String subject = String.format("✅ %d Message%s Deployed Successfully",
            messageCount, messageCount == 1 ? "" : "s");

        String textBody = String.format("""
            Good news! Your message%s been deployed to the blockchain.

            Deployment Summary:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Messages Deployed: %d
            Deployment Time: %s
            Status: ✅ LOCKED

            Your message%s now cryptographically secured and time-locked.
            %s will remain inaccessible until the specified unlock date.

            What this means:
            • Encryption keys split across multiple blockchains
            • Time-lock activated (mathematically guaranteed)
            • Permanent storage on Arweave
            • You'll receive an email when your message unlocks

            View your messages: https://tresor.io/messages

            Questions? support@tresor.io

            Best regards,
            The Tresor Team
            """,
            messageCount == 1 ? " has" : "s have",
            messageCount,
            deploymentTime.format(FORMATTER),
            messageCount == 1 ? " is" : "s are",
            messageCount == 1 ? "It" : "They");

        Email email = Email.builder()
            .to(toEmail)
            .from(FROM_EMAIL)
            .subject(subject)
            .textBody(textBody)
            .build();

        return sendEmail(email);
    }

    private String getTierDescription(String tier) {
        return switch (tier.toLowerCase()) {
            case "budget" -> """
                Budget Tier ($0.50-$0.57)
                • Blockchains: Polygon, Base, Optimism
                • Deployment: Within 24 hours (batched)
                • Perfect for: Personal time capsules
                """;
            case "standard" -> """
                Standard Tier ($3.00)
                • Blockchains: Arbitrum, Polygon, Avalanche
                • Deployment: Instant
                • Perfect for: Time-sensitive messages
                """;
            case "premium" -> """
                Premium Tier ($13.00) - BITCOIN SECURED
                • Blockchains: Bitcoin, Ethereum, Arbitrum
                • Deployment: Instant
                • Perfect for: Legal wills, important documents
                • Marketing: "Secured by Bitcoin Blockchain"
                """;
            case "enterprise" -> """
                Enterprise Tier ($25.00)
                • Blockchains: 7 chains (5-of-7 threshold)
                • Deployment: Instant
                • Perfect for: Business-critical documents
                • Maximum redundancy and SLA guarantee
                """;
            default -> "Custom tier";
        };
    }

    /**
     * Get all sent emails (for testing).
     */
    public List<Email> getSentEmails() {
        return new ArrayList<>(sentEmails);
    }

    /**
     * Clear sent emails (for testing).
     */
    public void clearSentEmails() {
        sentEmails.clear();
    }

    /**
     * Get sent email count (for testing).
     */
    public int getSentEmailCount() {
        return sentEmails.size();
    }

    /**
     * Get last sent email (for testing).
     */
    public Email getLastSentEmail() {
        return sentEmails.isEmpty() ? null : sentEmails.get(sentEmails.size() - 1);
    }
}
