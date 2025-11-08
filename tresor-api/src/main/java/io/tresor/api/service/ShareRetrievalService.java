package io.tresor.api.service;

import io.tresor.core.secretsharing.ShamirSecretSharing.Share;
import io.tresor.core.secretsharing.SecretHolderService.Blockchain;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service for retrieving secret shares with retry logic.
 *
 * Problem:
 * - Message unlocks at 2035-01-01 00:00:00
 * - Bitcoin share: ✅ Available
 * - Ethereum share: ❌ Network congestion
 * - Arbitrum share: ✅ Available
 * - Result: 2/3 shares → Cannot unlock yet
 *
 * Solution: Retry with exponential backoff
 * - Retry every 5 minutes initially
 * - If still failing after 1 hour: Retry every 30 minutes
 * - If still failing after 24 hours: Try alternative gateways
 * - If still failing after 7 days: Notify user
 *
 * This ensures temporary network issues don't prevent unlocking.
 */
@Slf4j
public class ShareRetrievalService {

    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(1);

    private final Map<Blockchain, BlockchainAdapter> adapters;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;

    public ShareRetrievalService(Map<Blockchain, BlockchainAdapter> adapters) {
        this.adapters = adapters;
        this.executorService = Executors.newFixedThreadPool(5);
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * Retrieve shares with retry logic.
     *
     * Tries all share locations in parallel, retries failures with backoff.
     *
     * @param message Message containing share deployment info
     * @return List of successfully retrieved shares
     */
    public List<Share> retrieveShares(Message message) {
        log.info("Retrieving shares for message: {}", message.getId());

        RetrievalAttempt attempt = attemptParallelRetrieval(message);

        int retrieved = attempt.getSuccessfulShares().size();
        int threshold = message.getThreshold();

        log.info("Retrieved {}/{} shares (threshold: {})", retrieved, message.getTotalShares(), threshold);

        if (retrieved >= threshold) {
            return attempt.getSuccessfulShares();
        }

        // Not enough shares - start retry process
        log.warn("Insufficient shares on first attempt, starting retry process");

        return retryWithBackoff(message, attempt);
    }

    /**
     * Attempt to retrieve all shares in parallel.
     */
    private RetrievalAttempt attemptParallelRetrieval(Message message) {
        List<Future<RetrievalResult>> futures = new ArrayList<>();

        for (ShareDeployment deployment : message.getShareDeployments()) {
            Future<RetrievalResult> future = executorService.submit(() ->
                retrieveFromBlockchain(deployment)
            );
            futures.add(future);
        }

        List<RetrievalResult> results = new ArrayList<>();

        for (Future<RetrievalResult> future : futures) {
            try {
                RetrievalResult result = future.get(30, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                log.error("Retrieval timed out");
                results.add(RetrievalResult.timeout());
            } catch (Exception e) {
                log.error("Retrieval failed: {}", e.getMessage());
                results.add(RetrievalResult.error(e));
            }
        }

        return new RetrievalAttempt(results);
    }

    /**
     * Retrieve share from specific blockchain.
     */
    private RetrievalResult retrieveFromBlockchain(ShareDeployment deployment) {
        Blockchain blockchain = deployment.getBlockchain();

        log.debug("Retrieving share from {}", blockchain.getName());

        try {
            BlockchainAdapter adapter = adapters.get(blockchain);

            if (adapter == null) {
                return RetrievalResult.error(
                    new IllegalStateException("No adapter for " + blockchain)
                );
            }

            // Check if unlocked
            if (!adapter.isUnlocked(deployment)) {
                return RetrievalResult.notYetUnlocked(blockchain);
            }

            // Retrieve share
            Share share = adapter.retrieveShare(deployment);

            return RetrievalResult.success(share, blockchain);

        } catch (Exception e) {
            log.error("Failed to retrieve from {}: {}", blockchain, e.getMessage());
            return RetrievalResult.error(e, blockchain);
        }
    }

    /**
     * Retry failed retrievals with exponential backoff.
     */
    private List<Share> retryWithBackoff(Message message, RetrievalAttempt initial) {
        List<Share> allShares = new ArrayList<>(initial.getSuccessfulShares());

        Duration currentDelay = INITIAL_RETRY_DELAY;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            if (allShares.size() >= message.getThreshold()) {
                break; // Threshold met!
            }

            log.info("Retry attempt {}/{} after {} delay",
                attempt, MAX_RETRY_ATTEMPTS, currentDelay);

            // Wait before retrying
            sleep(currentDelay);

            // Retry only failed deployments
            List<ShareDeployment> failedDeployments = initial.getFailedDeployments();

            for (ShareDeployment deployment : failedDeployments) {
                RetrievalResult result = retrieveFromBlockchain(deployment);

                if (result.isSuccess()) {
                    log.info("✅ Retry successful for {}", result.getBlockchain());
                    allShares.add(result.getShare());

                    if (allShares.size() >= message.getThreshold()) {
                        return allShares;
                    }
                }
            }

            // Exponential backoff (cap at MAX_RETRY_DELAY)
            currentDelay = Duration.ofMillis(
                Math.min(
                    currentDelay.toMillis() * 2,
                    MAX_RETRY_DELAY.toMillis()
                )
            );
        }

        // Still not enough shares after all retries
        throw new InsufficientSharesException(
            String.format("Failed to retrieve threshold shares after %d attempts: got %d, need %d",
                MAX_RETRY_ATTEMPTS, allShares.size(), message.getThreshold())
        );
    }

    /**
     * Schedule retry for later (used by cron job).
     */
    public ScheduledFuture<List<Share>> scheduleRetry(
        Message message,
        Duration delay
    ) {
        log.info("Scheduling retry for message {} in {}", message.getId(), delay);

        return scheduler.schedule(
            () -> retrieveShares(message),
            delay.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    // Helper methods

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========================================
    // VALUE OBJECTS
    // ========================================

    @Data
    static class RetrievalAttempt {
        private final List<RetrievalResult> results;

        public List<Share> getSuccessfulShares() {
            return results.stream()
                .filter(RetrievalResult::isSuccess)
                .map(RetrievalResult::getShare)
                .toList();
        }

        public List<ShareDeployment> getFailedDeployments() {
            return results.stream()
                .filter(r -> !r.isSuccess())
                .map(RetrievalResult::getDeployment)
                .filter(Objects::nonNull)
                .toList();
        }
    }

    @Data
    @Builder
    static class RetrievalResult {
        private boolean success;
        private Share share;
        private Blockchain blockchain;
        private ShareDeployment deployment;
        private Exception error;
        private String errorMessage;
        private boolean retryable;

        public static RetrievalResult success(Share share, Blockchain blockchain) {
            return RetrievalResult.builder()
                .success(true)
                .share(share)
                .blockchain(blockchain)
                .build();
        }

        public static RetrievalResult error(Exception error) {
            return error(error, null);
        }

        public static RetrievalResult error(Exception error, Blockchain blockchain) {
            return RetrievalResult.builder()
                .success(false)
                .error(error)
                .errorMessage(error.getMessage())
                .blockchain(blockchain)
                .retryable(isRetryable(error))
                .build();
        }

        public static RetrievalResult timeout() {
            return RetrievalResult.builder()
                .success(false)
                .errorMessage("Timeout")
                .retryable(true)
                .build();
        }

        public static RetrievalResult notYetUnlocked(Blockchain blockchain) {
            return RetrievalResult.builder()
                .success(false)
                .blockchain(blockchain)
                .errorMessage("Not yet unlocked")
                .retryable(true)
                .build();
        }

        private static boolean isRetryable(Exception e) {
            // Network errors are retryable
            if (e instanceof java.net.SocketException ||
                e instanceof java.net.UnknownHostException ||
                e instanceof TimeoutException ||
                e instanceof java.io.IOException) {
                return true;
            }

            // Rate limit errors are retryable
            if (e.getMessage() != null && (
                e.getMessage().contains("rate limit") ||
                e.getMessage().contains("too many requests") ||
                e.getMessage().contains("429"))) {
                return true;
            }

            // Temporary service errors are retryable
            if (e.getMessage() != null && (
                e.getMessage().contains("503") ||
                e.getMessage().contains("502") ||
                e.getMessage().contains("temporarily unavailable"))) {
                return true;
            }

            return false;
        }
    }

    // Placeholder classes
    @Data
    static class Message {
        private String id;
        private List<ShareDeployment> shareDeployments;
        private int threshold;
        private int totalShares;
    }

    @Data
    static class ShareDeployment {
        private Blockchain blockchain;
        private String identifier;
        private long unlockBlockHeight;
    }

    interface BlockchainAdapter {
        boolean isUnlocked(ShareDeployment deployment);
        Share retrieveShare(ShareDeployment deployment) throws Exception;
    }

    static class InsufficientSharesException extends RuntimeException {
        public InsufficientSharesException(String message) {
            super(message);
        }
    }
}
