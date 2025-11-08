package io.tresor.api.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock payment service for testing and development.
 *
 * In production, replace with StripePaymentService or PayPalPaymentService.
 *
 * This implementation:
 * - Simulates Stripe-style payment flow
 * - Stores payments in memory
 * - Auto-confirms payments
 * - Provides test card numbers
 * - Logs all payment operations
 */
@Slf4j
@Service
public class MockPaymentService implements PaymentService {

    private final Map<String, PaymentIntent> paymentIntents = new ConcurrentHashMap<>();
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, Refund> refunds = new ConcurrentHashMap<>();

    // Test card numbers
    public static final String TEST_CARD_SUCCESS = "4242424242424242";
    public static final String TEST_CARD_DECLINE = "4000000000000002";
    public static final String TEST_CARD_INSUFFICIENT_FUNDS = "4000000000009995";

    @Override
    public PaymentIntent createPaymentIntent(PaymentRequest request) throws PaymentException {
        log.info("Creating payment intent...");
        log.info("Amount: {}", request.getAmount());
        log.info("Currency: {}", request.getCurrency());
        log.info("Customer: {}", request.getCustomerEmail());
        log.info("Description: {}", request.getDescription());
        log.info("Tier: {}", request.getDeploymentTier());

        String intentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret = intentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        PaymentIntent intent = PaymentIntent.builder()
            .id(intentId)
            .amount(request.getAmount())
            .currency(request.getCurrency() != null ? request.getCurrency() : "usd")
            .status(PaymentStatus.REQUIRES_PAYMENT_METHOD)
            .clientSecret(clientSecret)
            .createdAt(LocalDateTime.now())
            .metadata(request.getMetadata())
            .build();

        paymentIntents.put(intentId, intent);

        log.info("Payment intent created: {}", intentId);
        log.info("Client secret: {}", clientSecret);

        return intent;
    }

    @Override
    public Payment confirmPayment(String paymentIntentId) throws PaymentException {
        log.info("Confirming payment intent: {}", paymentIntentId);

        PaymentIntent intent = paymentIntents.get(paymentIntentId);
        if (intent == null) {
            throw new PaymentException("Payment intent not found: " + paymentIntentId);
        }

        // Simulate payment processing
        try {
            Thread.sleep(500); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Auto-succeed for mock
        String paymentId = "py_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        Payment payment = Payment.builder()
            .id(paymentId)
            .paymentIntentId(paymentIntentId)
            .amount(intent.getAmount())
            .currency(intent.getCurrency())
            .status(PaymentStatus.SUCCEEDED)
            .customerEmail(intent.getMetadata() != null ? intent.getMetadata().get("customerEmail") : null)
            .description("Tresor message - " + (intent.getMetadata() != null ? intent.getMetadata().get("tier") : ""))
            .createdAt(intent.getCreatedAt())
            .confirmedAt(LocalDateTime.now())
            .receiptUrl("https://tresor.io/receipts/" + paymentId)
            .metadata(intent.getMetadata())
            .build();

        payments.put(paymentId, payment);
        intent.setStatus(PaymentStatus.SUCCEEDED);

        log.info("Payment confirmed successfully");
        log.info("Payment ID: {}", paymentId);
        log.info("Amount: {}", payment.getAmount());
        log.info("Status: {}", payment.getStatus());
        log.info("Receipt: {}", payment.getReceiptUrl());

        return payment;
    }

    @Override
    public Payment getPayment(String paymentId) throws PaymentException {
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new PaymentException("Payment not found: " + paymentId);
        }
        return payment;
    }

    @Override
    public Refund refundPayment(String paymentId, Double amount) throws PaymentException {
        log.info("Processing refund for payment: {}", paymentId);

        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new PaymentException("Payment not found: " + paymentId);
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new PaymentException("Can only refund succeeded payments");
        }

        double refundAmount = amount != null ? amount : payment.getAmount();

        if (refundAmount > payment.getAmount()) {
            throw new PaymentException("Refund amount exceeds payment amount");
        }

        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        Refund refund = Refund.builder()
            .id(refundId)
            .paymentId(paymentId)
            .amount(refundAmount)
            .currency(payment.getCurrency())
            .status(RefundStatus.SUCCEEDED)
            .reason("Requested by customer")
            .createdAt(LocalDateTime.now())
            .build();

        refunds.put(refundId, refund);

        log.info("Refund processed successfully");
        log.info("Refund ID: {}", refundId);
        log.info("Amount: {}", refundAmount);

        return refund;
    }

    @Override
    public boolean handleWebhook(String payload, String signature) {
        log.info("Webhook received");
        log.info("Payload: {}", payload.substring(0, Math.min(payload.length(), 100)));
        log.info("Signature: {}", signature);

        // In production, verify signature and process events
        // For mock, just log and return true
        return true;
    }

    /**
     * Helper method to create and confirm payment in one step (for testing).
     */
    public Payment createAndConfirmPayment(double amount, String customerEmail, String tier, String messageId)
            throws PaymentException {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("customerEmail", customerEmail);
        metadata.put("tier", tier);
        metadata.put("messageId", messageId);

        PaymentRequest request = PaymentRequest.builder()
            .amount(amount)
            .currency("usd")
            .customerEmail(customerEmail)
            .description("Tresor message - " + tier + " tier")
            .deploymentTier(tier)
            .messageId(messageId)
            .metadata(metadata)
            .build();

        PaymentIntent intent = createPaymentIntent(request);
        return confirmPayment(intent.getId());
    }

    /**
     * Get all payments (for testing).
     */
    public Map<String, Payment> getAllPayments() {
        return new HashMap<>(payments);
    }

    /**
     * Get all refunds (for testing).
     */
    public Map<String, Refund> getAllRefunds() {
        return new HashMap<>(refunds);
    }

    /**
     * Clear all data (for testing).
     */
    public void clearAll() {
        paymentIntents.clear();
        payments.clear();
        refunds.clear();
    }

    /**
     * Simulate payment failure (for testing).
     */
    public void simulatePaymentFailure() {
        // Implementation for testing payment failures
        log.warn("Payment failure simulation enabled");
    }
}
