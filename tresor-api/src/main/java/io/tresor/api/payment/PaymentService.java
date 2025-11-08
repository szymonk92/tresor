package io.tresor.api.payment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payment service for processing payments.
 *
 * Supports multiple payment providers:
 * - Stripe (production)
 * - PayPal (production)
 * - Mock (testing)
 */
public interface PaymentService {

    /**
     * Create payment intent.
     *
     * @param request Payment request
     * @return Payment intent with client secret
     */
    PaymentIntent createPaymentIntent(PaymentRequest request) throws PaymentException;

    /**
     * Confirm payment.
     *
     * @param paymentIntentId Payment intent ID
     * @return Confirmed payment
     */
    Payment confirmPayment(String paymentIntentId) throws PaymentException;

    /**
     * Get payment status.
     *
     * @param paymentId Payment ID
     * @return Payment details
     */
    Payment getPayment(String paymentId) throws PaymentException;

    /**
     * Refund payment.
     *
     * @param paymentId Payment ID
     * @param amount Amount to refund (null for full refund)
     * @return Refund details
     */
    Refund refundPayment(String paymentId, Double amount) throws PaymentException;

    /**
     * Handle webhook event.
     *
     * @param payload Webhook payload
     * @param signature Webhook signature
     * @return true if handled successfully
     */
    boolean handleWebhook(String payload, String signature);

    /**
     * Payment request.
     */
    @Data
    @Builder
    class PaymentRequest {
        private double amount;
        private String currency;
        private String customerEmail;
        private String description;
        private String deploymentTier;
        private String messageId;
        private Map<String, String> metadata;
    }

    /**
     * Payment intent (Stripe-style).
     */
    @Data
    @Builder
    class PaymentIntent {
        private String id;
        private double amount;
        private String currency;
        private PaymentStatus status;
        private String clientSecret;
        private LocalDateTime createdAt;
        private Map<String, String> metadata;
    }

    /**
     * Payment details.
     */
    @Data
    @Builder
    class Payment {
        private String id;
        private String paymentIntentId;
        private double amount;
        private String currency;
        private PaymentStatus status;
        private String customerEmail;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime confirmedAt;
        private String receiptUrl;
        private Map<String, String> metadata;
    }

    /**
     * Refund details.
     */
    @Data
    @Builder
    class Refund {
        private String id;
        private String paymentId;
        private double amount;
        private String currency;
        private RefundStatus status;
        private String reason;
        private LocalDateTime createdAt;
    }

    /**
     * Payment status.
     */
    enum PaymentStatus {
        PENDING,
        REQUIRES_PAYMENT_METHOD,
        REQUIRES_CONFIRMATION,
        REQUIRES_ACTION,
        PROCESSING,
        SUCCEEDED,
        CANCELED,
        FAILED
    }

    /**
     * Refund status.
     */
    enum RefundStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        CANCELED
    }

    /**
     * Payment exception.
     */
    class PaymentException extends Exception {
        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
