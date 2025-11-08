package io.tresor.api.controller;

import io.tresor.api.payment.PaymentService;
import io.tresor.api.payment.PaymentService.Payment;
import io.tresor.api.payment.PaymentService.PaymentIntent;
import io.tresor.api.payment.PaymentService.PaymentRequest;
import io.tresor.api.payment.PaymentService.Refund;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for payment operations.
 *
 * Endpoints:
 * - POST /api/payments/intent - Create payment intent
 * - POST /api/payments/{id}/confirm - Confirm payment
 * - GET  /api/payments/{id} - Get payment status
 * - POST /api/payments/{id}/refund - Refund payment
 * - POST /api/payments/webhook - Handle payment webhooks
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create payment intent.
     *
     * POST /api/payments/intent
     * {
     *   "amount": 0.50,
     *   "currency": "usd",
     *   "customerEmail": "user@example.com",
     *   "description": "Tresor message - budget tier",
     *   "deploymentTier": "budget",
     *   "messageId": "msg-123"
     * }
     */
    @PostMapping("/intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody CreatePaymentIntentRequest request) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("customerEmail", request.getCustomerEmail());
            metadata.put("tier", request.getDeploymentTier());
            metadata.put("messageId", request.getMessageId());

            PaymentRequest paymentRequest = PaymentRequest.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "usd")
                .customerEmail(request.getCustomerEmail())
                .description(request.getDescription())
                .deploymentTier(request.getDeploymentTier())
                .messageId(request.getMessageId())
                .metadata(metadata)
                .build();

            PaymentIntent intent = paymentService.createPaymentIntent(paymentRequest);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentIntent", Map.of(
                    "id", intent.getId(),
                    "clientSecret", intent.getClientSecret(),
                    "amount", intent.getAmount(),
                    "currency", intent.getCurrency(),
                    "status", intent.getStatus().toString()
                )
            ));

        } catch (PaymentService.PaymentException e) {
            log.error("Failed to create payment intent: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Confirm payment.
     *
     * POST /api/payments/{id}/confirm
     */
    @PostMapping("/{paymentIntentId}/confirm")
    public ResponseEntity<?> confirmPayment(@PathVariable String paymentIntentId) {
        try {
            Payment payment = paymentService.confirmPayment(paymentIntentId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "payment", Map.of(
                    "id", payment.getId(),
                    "amount", payment.getAmount(),
                    "currency", payment.getCurrency(),
                    "status", payment.getStatus().toString(),
                    "receiptUrl", payment.getReceiptUrl(),
                    "confirmedAt", payment.getConfirmedAt().toString()
                )
            ));

        } catch (PaymentService.PaymentException e) {
            log.error("Failed to confirm payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get payment status.
     *
     * GET /api/payments/{id}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable String paymentId) {
        try {
            Payment payment = paymentService.getPayment(paymentId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "payment", Map.of(
                    "id", payment.getId(),
                    "amount", payment.getAmount(),
                    "currency", payment.getCurrency(),
                    "status", payment.getStatus().toString(),
                    "customerEmail", payment.getCustomerEmail(),
                    "description", payment.getDescription(),
                    "receiptUrl", payment.getReceiptUrl(),
                    "createdAt", payment.getCreatedAt().toString()
                )
            ));

        } catch (PaymentService.PaymentException e) {
            log.error("Payment not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Refund payment.
     *
     * POST /api/payments/{id}/refund
     * {
     *   "amount": 0.50  // Optional, full refund if not specified
     * }
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(
        @PathVariable String paymentId,
        @RequestBody(required = false) RefundRequest request
    ) {
        try {
            Double amount = request != null ? request.getAmount() : null;
            Refund refund = paymentService.refundPayment(paymentId, amount);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "refund", Map.of(
                    "id", refund.getId(),
                    "paymentId", refund.getPaymentId(),
                    "amount", refund.getAmount(),
                    "currency", refund.getCurrency(),
                    "status", refund.getStatus().toString(),
                    "createdAt", refund.getCreatedAt().toString()
                )
            ));

        } catch (PaymentService.PaymentException e) {
            log.error("Failed to refund payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Handle payment webhook (Stripe/PayPal).
     *
     * POST /api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
        @RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        try {
            boolean handled = paymentService.handleWebhook(payload, signature);

            if (handled) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false));
            }

        } catch (Exception e) {
            log.error("Failed to handle webhook: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // DTOs

    @Data
    static class CreatePaymentIntentRequest {
        private double amount;
        private String currency;
        private String customerEmail;
        private String description;
        private String deploymentTier;
        private String messageId;
    }

    @Data
    static class RefundRequest {
        private Double amount;
    }
}
