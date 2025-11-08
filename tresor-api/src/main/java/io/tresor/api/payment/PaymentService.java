package io.tresor.api.payment;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment service with pre-authorization to prevent money loss.
 *
 * Problem solved:
 * - Deployment costs money (Bitcoin fees, gas, etc.)
 * - If deployment succeeds but payment fails → company loses money
 *
 * Solution:
 * - Pre-authorize payment BEFORE deployment
 * - Capture actual amount after successful deployment
 * - Void authorization if deployment fails
 *
 * Flow:
 * 1. Estimate deployment cost
 * 2. Pre-authorize (hold) 120% of estimate
 * 3. Deploy message
 * 4. Capture actual cost
 * 5. Release remaining hold
 */
@Slf4j
public interface PaymentService {

    /**
     * Pre-authorize (hold) funds without charging.
     *
     * This creates a temporary hold on the user's payment method
     * without actually charging them. The hold typically lasts 7 days.
     *
     * @param paymentMethod User's payment method (card, account balance, etc.)
     * @param amount Amount to authorize
     * @param description What this payment is for
     * @return Authorization that can be captured or voided
     * @throws PaymentException if authorization fails
     */
    PaymentAuthorization authorize(
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String description
    ) throws PaymentException;

    /**
     * Capture (charge) a previously authorized payment.
     *
     * This actually charges the user's payment method.
     * Can capture less than authorized amount (e.g., actual cost < estimate).
     *
     * @param authorization The pre-authorization
     * @param actualAmount Actual amount to charge (≤ authorized amount)
     * @return Payment confirmation
     * @throws PaymentException if capture fails
     */
    PaymentConfirmation capture(
        PaymentAuthorization authorization,
        BigDecimal actualAmount
    ) throws PaymentException;

    /**
     * Void (cancel) a pre-authorization without charging.
     *
     * Use this if deployment fails and you don't want to charge the user.
     *
     * @param authorization The authorization to void
     * @throws PaymentException if void fails
     */
    void voidAuthorization(PaymentAuthorization authorization) throws PaymentException;

    /**
     * Refund a captured payment.
     *
     * @param confirmation The payment to refund
     * @param amount Amount to refund (can be partial)
     * @param reason Reason for refund
     * @return Refund confirmation
     * @throws PaymentException if refund fails
     */
    RefundConfirmation refund(
        PaymentConfirmation confirmation,
        BigDecimal amount,
        String reason
    ) throws PaymentException;

    /**
     * Get current balance for balance-based payment method.
     *
     * @param userId User ID
     * @return Current balance
     */
    BigDecimal getBalance(String userId);

    /**
     * Add funds to user's balance.
     *
     * @param userId User ID
     * @param amount Amount to add
     * @param paymentMethod Payment method for top-up
     * @return New balance
     * @throws PaymentException if top-up fails
     */
    BigDecimal addFunds(
        String userId,
        BigDecimal amount,
        PaymentMethod paymentMethod
    ) throws PaymentException;

    // ========================================
    // VALUE OBJECTS
    // ========================================

    @Data
    @Builder
    class PaymentAuthorization {
        private String authorizationId;
        private PaymentMethod paymentMethod;
        private BigDecimal authorizedAmount;
        private String description;
        private LocalDateTime authorizedAt;
        private LocalDateTime expiresAt;
        private AuthorizationStatus status;
        private String externalTransactionId; // Stripe, PayPal, etc.

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public boolean canCapture() {
            return status == AuthorizationStatus.AUTHORIZED && !isExpired();
        }
    }

    @Data
    @Builder
    class PaymentConfirmation {
        private String confirmationId;
        private String authorizationId;
        private PaymentMethod paymentMethod;
        private BigDecimal amount;
        private String description;
        private LocalDateTime chargedAt;
        private String externalTransactionId;
        private PaymentStatus status;
    }

    @Data
    @Builder
    class RefundConfirmation {
        private String refundId;
        private String confirmationId;
        private BigDecimal amount;
        private String reason;
        private LocalDateTime refundedAt;
        private RefundStatus status;
    }

    @Data
    @Builder
    class PaymentMethod {
        private String id;
        private PaymentType type;
        private String last4; // Last 4 digits of card
        private String brand; // Visa, MasterCard, etc.
        private String userId;
        private boolean isDefault;
    }

    enum PaymentType {
        CREDIT_CARD,
        DEBIT_CARD,
        ACCOUNT_BALANCE,
        PAYPAL,
        CRYPTO
    }

    enum AuthorizationStatus {
        AUTHORIZED,    // Hold is active
        CAPTURED,      // Payment captured
        VOIDED,        // Authorization cancelled
        EXPIRED        // Hold expired
    }

    enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }

    enum RefundStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    /**
     * Exception thrown when payment operations fail.
     */
    class PaymentException extends Exception {
        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }

        public static class InsufficientFundsException extends PaymentException {
            public InsufficientFundsException(BigDecimal required, BigDecimal available) {
                super(String.format("Insufficient funds: need $%s, have $%s",
                    required, available));
            }
        }

        public static class CardDeclinedException extends PaymentException {
            public CardDeclinedException(String reason) {
                super("Card declined: " + reason);
            }
        }

        public static class AuthorizationExpiredException extends PaymentException {
            public AuthorizationExpiredException() {
                super("Authorization has expired");
            }
        }
    }
}
