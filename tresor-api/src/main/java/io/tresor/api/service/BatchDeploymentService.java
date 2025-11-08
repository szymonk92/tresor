package io.tresor.api.service;

import io.tresor.api.model.Message;
import io.tresor.api.model.Message.MessageStatus;
import io.tresor.api.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Batch deployment service for cost optimization.
 *
 * Instead of deploying messages immediately (expensive), we batch them
 * and deploy at scheduled times (e.g., daily at midnight).
 *
 * Cost Savings:
 * - Individual deployment: Each message pays full blockchain fees
 * - Batch deployment: Amortize costs across multiple messages
 * - Example: 100 messages batched = ~30% cost reduction
 *
 * How it works:
 * 1. Messages in "budget" tier are set to PENDING_BATCH status
 * 2. Cron job runs daily at midnight
 * 3. Find all PENDING_BATCH messages
 * 4. Group by deployment tier
 * 5. Deploy in batches to blockchains
 * 6. Update status to LOCKED
 */
@Slf4j
@Service
public class BatchDeploymentService {

    private final MessageRepository messageRepository;
    private final MessageService messageService;

    public BatchDeploymentService(
        MessageRepository messageRepository,
        MessageService messageService
    ) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
    }

    /**
     * Run batch deployment every day at midnight.
     *
     * Cron: 0 0 0 * * * = "At 00:00:00 every day"
     */
    @Scheduled(cron = "${tresor.batch.schedule-cron:0 0 0 * * *}")
    @Transactional
    public void runBatchDeployment() {
        log.info("╔═══════════════════════════════════════════════════════╗");
        log.info("║       BATCH DEPLOYMENT STARTED                        ║");
        log.info("╚═══════════════════════════════════════════════════════╝");
        log.info("Time: {}", LocalDateTime.now());

        try {
            // Find all messages pending batch deployment
            List<Message> pendingMessages = messageRepository.findByStatus(MessageStatus.PENDING_BATCH);

            if (pendingMessages.isEmpty()) {
                log.info("No messages pending batch deployment");
                return;
            }

            log.info("Found {} messages pending deployment", pendingMessages.size());

            // Group by deployment tier
            Map<String, List<Message>> messagesByTier = pendingMessages.stream()
                .collect(Collectors.groupingBy(Message::getDeploymentTier));

            int totalDeployed = 0;
            int totalFailed = 0;
            double totalCost = 0.0;

            // Deploy each tier's batch
            for (Map.Entry<String, List<Message>> entry : messagesByTier.entrySet()) {
                String tier = entry.getKey();
                List<Message> messages = entry.getValue();

                log.info("─────────────────────────────────────────────────────");
                log.info("Deploying {} messages for tier: {}", messages.size(), tier);

                int deployed = 0;
                int failed = 0;

                for (Message message : messages) {
                    try {
                        // Deploy the message
                        deployMessage(message);
                        deployed++;
                        totalCost += message.getCostUsd();

                        log.info("✅ Deployed message {} (cost: ${})",
                            message.getId(), String.format("%.2f", message.getCostUsd()));

                    } catch (Exception e) {
                        failed++;
                        message.setStatus(MessageStatus.FAILED);
                        message.setErrorMessage("Batch deployment failed: " + e.getMessage());
                        messageRepository.save(message);

                        log.error("❌ Failed to deploy message {}: {}",
                            message.getId(), e.getMessage());
                    }
                }

                totalDeployed += deployed;
                totalFailed += failed;

                log.info("Tier {} complete: {} deployed, {} failed", tier, deployed, failed);
            }

            // Summary
            log.info("╔═══════════════════════════════════════════════════════╗");
            log.info("║       BATCH DEPLOYMENT COMPLETE                       ║");
            log.info("╚═══════════════════════════════════════════════════════╝");
            log.info("✅ Successfully deployed: {}", totalDeployed);
            log.info("❌ Failed: {}", totalFailed);
            log.info("💰 Total cost: ${}", String.format("%.2f", totalCost));
            log.info("📊 Average cost per message: ${}",
                String.format("%.2f", totalDeployed > 0 ? totalCost / totalDeployed : 0));

        } catch (Exception e) {
            log.error("Batch deployment failed with unexpected error", e);
        }
    }

    /**
     * Deploy a single message (called during batch).
     */
    private void deployMessage(Message message) throws Exception {
        log.debug("Deploying message: id={}, mode={}, tier={}",
            message.getId(), message.getEncryptionMode(), message.getDeploymentTier());

        // Update status to DEPLOYING
        message.setStatus(MessageStatus.DEPLOYING);
        messageRepository.save(message);

        // For PASSWORD_ENCRYPTION and NO_ENCRYPTION modes:
        // No blockchain deployment needed, just mark as LOCKED
        if ("PASSWORD_ENCRYPTION".equals(message.getEncryptionMode()) ||
            "NO_ENCRYPTION".equals(message.getEncryptionMode())) {

            message.setStatus(MessageStatus.LOCKED);
            messageRepository.save(message);
            return;
        }

        // For FULL_ENCRYPTION mode:
        // Deploy Shamir shares to blockchains
        // TODO: Actual blockchain deployment
        // For now, just simulate deployment

        // Simulate deployment delay
        Thread.sleep(100);

        // Mark as successfully deployed
        message.setStatus(MessageStatus.LOCKED);
        messageRepository.save(message);

        log.debug("Message {} deployed successfully", message.getId());
    }

    /**
     * Manually trigger batch deployment (for testing).
     */
    public BatchDeploymentResult triggerBatchDeployment() {
        log.info("Manual batch deployment triggered");

        List<Message> pendingMessages = messageRepository.findByStatus(MessageStatus.PENDING_BATCH);

        if (pendingMessages.isEmpty()) {
            return new BatchDeploymentResult(0, 0, 0.0, "No messages pending deployment");
        }

        int deployed = 0;
        int failed = 0;
        double totalCost = 0.0;

        for (Message message : pendingMessages) {
            try {
                deployMessage(message);
                deployed++;
                totalCost += message.getCostUsd();
            } catch (Exception e) {
                failed++;
                message.setStatus(MessageStatus.FAILED);
                message.setErrorMessage("Manual deployment failed: " + e.getMessage());
                messageRepository.save(message);
            }
        }

        String summary = String.format("Deployed %d messages, %d failed, total cost: $%.2f",
            deployed, failed, totalCost);

        log.info(summary);

        return new BatchDeploymentResult(deployed, failed, totalCost, summary);
    }

    /**
     * Get batch deployment statistics.
     */
    public BatchDeploymentStats getStats() {
        long pendingCount = messageRepository.countByUserIdAndStatus("", MessageStatus.PENDING_BATCH);

        // Group pending messages by tier
        List<Message> pending = messageRepository.findByStatus(MessageStatus.PENDING_BATCH);
        Map<String, Long> pendingByTier = pending.stream()
            .collect(Collectors.groupingBy(Message::getDeploymentTier, Collectors.counting()));

        double estimatedCost = pending.stream()
            .mapToDouble(Message::getCostUsd)
            .sum();

        return new BatchDeploymentStats(
            pendingCount,
            pendingByTier,
            estimatedCost,
            LocalDateTime.now()
        );
    }

    /**
     * Result of batch deployment.
     */
    public record BatchDeploymentResult(
        int deployed,
        int failed,
        double totalCost,
        String summary
    ) {}

    /**
     * Batch deployment statistics.
     */
    public record BatchDeploymentStats(
        long pendingMessages,
        Map<String, Long> pendingByTier,
        double estimatedCost,
        LocalDateTime timestamp
    ) {}
}
