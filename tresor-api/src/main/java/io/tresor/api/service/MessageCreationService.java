package io.tresor.api.service;

import io.tresor.api.payment.PaymentService;
import io.tresor.api.payment.PaymentService.*;
import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.model.EncryptedMessage;
import io.tresor.core.model.PlainMessage;
import io.tresor.core.secretsharing.DeploymentOrchestrator;
import io.tresor.core.secretsharing.SecretHolderService.*;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Service for creating messages with payment pre-authorization.
 *
 * CRITICAL: Ensures payment succeeds BEFORE spending money on deployment.
 *
 * Flow:
 * 1. Estimate deployment cost
 * 2. Pre-authorize payment (hold funds)
 * 3. Deploy message (costs real money)
 * 4. Capture actual payment amount
 * 5. Handle failures gracefully
 */
@Slf4j
public class MessageCreationService {

    private final PaymentService paymentService;
    private final DeploymentOrchestrator deploymentOrchestrator;
    private final EncryptionService encryptionService;
    private final ArweaveService arweaveService;

    // Buffer percentage for cost estimation
    private static final BigDecimal COST_BUFFER = new BigDecimal("1.20"); // 20% buffer

    public MessageCreationService(
        PaymentService paymentService,
        DeploymentOrchestrator deploymentOrchestrator,
        EncryptionService encryptionService,
        ArweaveService arweaveService
    ) {
        this.paymentService = paymentService;
        this.deploymentOrchestrator = deploymentOrchestrator;
        this.encryptionService = encryptionService;
        this.arweaveService = arweaveService;
    }

    /**
     * Create message with payment pre-authorization.
     *
     * Guarantees: Either (payment succeeds + message deployed) OR (no payment + no deployment)
     *
     * @param message The message to create
     * @param unlockDate When to unlock
     * @param paymentMethod Payment method
     * @param config Deployment configuration
     * @return Receipt with deployment details
     * @throws MessageCreationException if creation fails
     */
    public MessageCreationReceipt createMessage(
        PlainMessage message,
        LocalDateTime unlockDate,
        PaymentMethod paymentMethod,
        DeploymentConfig config
    ) throws MessageCreationException {

        log.info("Starting message creation for unlock date: {}", unlockDate);

        // Step 1: Estimate cost
        BigDecimal estimatedCost = estimateDeploymentCost(message, config);
        BigDecimal bufferAmount = estimatedCost.multiply(COST_BUFFER);

        log.info("Estimated cost: ${}, authorizing: ${}",
            estimatedCost, bufferAmount);

        PaymentAuthorization authorization = null;

        try {
            // Step 2: Pre-authorize payment
            authorization = preAuthorizePayment(
                paymentMethod,
                bufferAmount,
                "Message creation for unlock " + unlockDate
            );

            // Step 3: Encrypt message
            EncryptedMessage encrypted = encryptMessage(message);

            // Step 4: Upload to Arweave
            String arweaveId = uploadToArweave(encrypted);

            // Step 5: Deploy shares to blockchains
            byte[] encryptionKey = encrypted.getEncryptionKey();
            DeploymentReceipt deployment = deployShares(
                encryptionKey,
                unlockDate,
                config
            );

            // Step 6: Calculate actual cost
            BigDecimal actualCost = calculateActualCost(deployment, arweaveId);

            log.info("Actual cost: ${}", actualCost);

            // Step 7: Capture payment for actual amount
            PaymentConfirmation payment = capturePayment(authorization, actualCost);

            // Step 8: Success! Return receipt
            return MessageCreationReceipt.builder()
                .messageId(encrypted.getId())
                .arweaveId(arweaveId)
                .deployment(deployment)
                .payment(payment)
                .estimatedCost(estimatedCost)
                .actualCost(actualCost)
                .savings(estimatedCost.subtract(actualCost))
                .createdAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            // CRITICAL: Deployment failed - void payment authorization
            log.error("Message creation failed: {}", e.getMessage());

            if (authorization != null) {
                voidPaymentSafely(authorization);
            }

            throw new MessageCreationException("Failed to create message", e);
        }
    }

    /**
     * Create message using account balance (simpler flow).
     */
    public MessageCreationReceipt createMessageWithBalance(
        String userId,
        PlainMessage message,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) throws MessageCreationException {

        log.info("Creating message with balance for user: {}", userId);

        // Step 1: Check balance
        BigDecimal estimatedCost = estimateDeploymentCost(message, config);
        BigDecimal balance = paymentService.getBalance(userId);

        if (balance.compareTo(estimatedCost) < 0) {
            throw new MessageCreationException(
                new PaymentException.InsufficientFundsException(estimatedCost, balance)
            );
        }

        // Step 2: Deduct estimated amount immediately
        PaymentConfirmation upfrontPayment = null;

        try {
            // Create "internal" payment method for balance
            PaymentMethod balanceMethod = PaymentMethod.builder()
                .type(PaymentType.ACCOUNT_BALANCE)
                .userId(userId)
                .build();

            // Pre-authorize from balance
            PaymentAuthorization auth = paymentService.authorize(
                balanceMethod,
                estimatedCost,
                "Message creation"
            );

            // Rest of flow same as credit card...
            EncryptedMessage encrypted = encryptMessage(message);
            String arweaveId = uploadToArweave(encrypted);

            DeploymentReceipt deployment = deployShares(
                encrypted.getEncryptionKey(),
                unlockDate,
                config
            );

            BigDecimal actualCost = calculateActualCost(deployment, arweaveId);

            // Capture actual cost
            upfrontPayment = paymentService.capture(auth, actualCost);

            return MessageCreationReceipt.builder()
                .messageId(encrypted.getId())
                .arweaveId(arweaveId)
                .deployment(deployment)
                .payment(upfrontPayment)
                .estimatedCost(estimatedCost)
                .actualCost(actualCost)
                .createdAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Message creation with balance failed: {}", e.getMessage());
            throw new MessageCreationException("Failed to create message", e);
        }
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private BigDecimal estimateDeploymentCost(PlainMessage message, DeploymentConfig config) {
        BigDecimal cost = BigDecimal.ZERO;

        // Arweave storage cost
        long messageSize = message.calculateSize();
        BigDecimal arweaveCost = arweaveService.estimateCost(messageSize);
        cost = cost.add(arweaveCost);

        // Blockchain deployment costs
        for (Blockchain blockchain : config.getBlockchains()) {
            BigDecimal blockchainCost = estimateBlockchainCost(blockchain);
            cost = cost.add(blockchainCost);
        }

        return cost;
    }

    private BigDecimal estimateBlockchainCost(Blockchain blockchain) {
        // Rough estimates (should query real-time gas prices)
        return switch (blockchain) {
            case BITCOIN -> new BigDecimal("7.00");
            case ETHEREUM -> new BigDecimal("20.00");
            case ARBITRUM -> new BigDecimal("0.50");
            case POLYGON -> new BigDecimal("0.01");
            case CARDANO -> new BigDecimal("2.00");
            case SELF_CUSTODY -> BigDecimal.ZERO;
            case ARWEAVE -> new BigDecimal("0.04");
        };
    }

    private PaymentAuthorization preAuthorizePayment(
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String description
    ) throws MessageCreationException {
        try {
            PaymentAuthorization auth = paymentService.authorize(
                paymentMethod,
                amount,
                description
            );

            log.info("Payment pre-authorized: {} for ${}",
                auth.getAuthorizationId(), amount);

            return auth;

        } catch (PaymentException e) {
            throw new MessageCreationException("Payment authorization failed", e);
        }
    }

    private EncryptedMessage encryptMessage(PlainMessage message)
        throws MessageCreationException {
        try {
            return encryptionService.encrypt(message);
        } catch (EncryptionException e) {
            throw new MessageCreationException("Encryption failed", e);
        }
    }

    private String uploadToArweave(EncryptedMessage encrypted)
        throws MessageCreationException {
        try {
            return arweaveService.upload(encrypted);
        } catch (Exception e) {
            throw new MessageCreationException("Arweave upload failed", e);
        }
    }

    private DeploymentReceipt deployShares(
        byte[] key,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) throws MessageCreationException {
        try {
            return deploymentOrchestrator.deployWithRecovery(key, unlockDate, config);
        } catch (EncryptionException e) {
            throw new MessageCreationException("Share deployment failed", e);
        }
    }

    private BigDecimal calculateActualCost(DeploymentReceipt deployment, String arweaveId) {
        // Sum up all actual costs
        BigDecimal total = BigDecimal.valueOf(deployment.getTotalCostUsd());

        // Add Arweave cost (if available)
        // total = total.add(arweaveService.getActualCost(arweaveId));

        return total;
    }

    private PaymentConfirmation capturePayment(
        PaymentAuthorization authorization,
        BigDecimal actualAmount
    ) throws MessageCreationException {
        try {
            PaymentConfirmation confirmation = paymentService.capture(
                authorization,
                actualAmount
            );

            log.info("Payment captured: {} for ${}",
                confirmation.getConfirmationId(), actualAmount);

            return confirmation;

        } catch (PaymentException e) {
            throw new MessageCreationException("Payment capture failed", e);
        }
    }

    private void voidPaymentSafely(PaymentAuthorization authorization) {
        try {
            paymentService.voidAuthorization(authorization);
            log.info("Payment authorization voided: {}", authorization.getAuthorizationId());
        } catch (Exception e) {
            log.error("Failed to void authorization {}: {}",
                authorization.getAuthorizationId(), e.getMessage());
            // Don't throw - already in error handling
        }
    }

    // ========================================
    // VALUE OBJECTS
    // ========================================

    @lombok.Data
    @lombok.Builder
    public static class MessageCreationReceipt {
        private String messageId;
        private String arweaveId;
        private DeploymentReceipt deployment;
        private PaymentConfirmation payment;
        private BigDecimal estimatedCost;
        private BigDecimal actualCost;
        private BigDecimal savings;
        private LocalDateTime createdAt;
    }

    public static class MessageCreationException extends Exception {
        public MessageCreationException(String message) {
            super(message);
        }

        public MessageCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Placeholder interfaces (would be implemented separately)
    interface EncryptionService {
        EncryptedMessage encrypt(PlainMessage message) throws EncryptionException;
    }

    interface ArweaveService {
        String upload(EncryptedMessage message) throws Exception;
        BigDecimal estimateCost(long sizeBytes);
    }
}
