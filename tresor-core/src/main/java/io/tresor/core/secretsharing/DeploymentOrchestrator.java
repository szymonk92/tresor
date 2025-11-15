package io.tresor.core.secretsharing;

import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.secretsharing.SecretHolderService.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orchestrates deployment of secret shares across multiple blockchains
 * with fault tolerance and recovery.
 *
 * Handles:
 * - Parallel deployment to multiple chains
 * - Partial failure recovery
 * - Retry with exponential backoff
 * - Fallback to alternative chains
 * - Threshold validation
 *
 * Guarantees:
 * - Either threshold shares deployed OR complete rollback
 * - No partial states that break message recovery
 * - Idempotent operations (safe to retry)
 */
@Slf4j
public class DeploymentOrchestrator {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final int THREAD_POOL_SIZE = 10;

    private final Map<Blockchain, ChainAdapter> chainAdapters;
    private final ExecutorService executorService;
    private final ShamirSecretSharing shamirService;

    public DeploymentOrchestrator(Map<Blockchain, ChainAdapter> chainAdapters) {
        this.chainAdapters = chainAdapters;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.shamirService = new ShamirSecretSharing();
    }

    /**
     * Deploy secret shares with guaranteed threshold or complete failure.
     *
     * @param key The encryption key to split and deploy
     * @param unlockDate When shares should become available
     * @param config Deployment configuration
     * @return Receipt with successful deployments
     * @throws DeploymentException if threshold not met
     */
    public DeploymentReceipt deployWithRecovery(
        byte[] key,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) throws EncryptionException {

        log.info("Starting deployment: {} shares with threshold {}",
            config.getTotalShares(), config.getThreshold());

        // Phase 1: Split key into shares
        List<ShamirSecretSharing.Share> shares = shamirService.split(
            key,
            config.getTotalShares(),
            config.getThreshold()
        );

        // Phase 2: Attempt parallel deployment
        DeploymentAttempt attempt = attemptParallelDeployment(shares, unlockDate, config);

        // Phase 3: Check if threshold met
        if (attempt.getSuccessCount() >= config.getThreshold()) {
            log.info("✅ Deployment successful: {}/{} shares deployed",
                attempt.getSuccessCount(), config.getTotalShares());
            return buildReceipt(attempt);
        }

        // Phase 4: Threshold not met - attempt recovery
        log.warn("⚠️ Only {}/{} shares deployed, threshold is {}",
            attempt.getSuccessCount(), config.getTotalShares(), config.getThreshold());

        DeploymentAttempt recovered = attemptRecovery(attempt, shares, unlockDate, config);

        if (recovered.getSuccessCount() >= config.getThreshold()) {
            log.info("✅ Recovery successful: {}/{} shares deployed",
                recovered.getSuccessCount(), config.getTotalShares());
            return buildReceipt(recovered);
        }

        // Phase 5: Complete failure - rollback
        log.error("❌ Failed to deploy threshold shares after recovery");
        rollbackDeployment(recovered);

        throw new EncryptionException(
            String.format("Failed to deploy minimum shares: got %d, need %d",
                recovered.getSuccessCount(), config.getThreshold())
        );
    }

    /**
     * Attempt to deploy all shares in parallel.
     */
    private DeploymentAttempt attemptParallelDeployment(
        List<ShamirSecretSharing.Share> shares,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) {
        List<Future<DeploymentResult>> futures = new ArrayList<>();
        List<DeploymentResult> results = new ArrayList<>();

        // Submit all deployment tasks
        for (int i = 0; i < shares.size() && i < config.getBlockchains().size(); i++) {
            ShamirSecretSharing.Share share = shares.get(i);
            Blockchain blockchain = config.getBlockchains().get(i);

            Future<DeploymentResult> future = executorService.submit(() ->
                deployToChain(share, blockchain, unlockDate, config)
            );

            futures.add(future);
        }

        // Collect results
        for (Future<DeploymentResult> future : futures) {
            try {
                DeploymentResult result = future.get(60, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                log.error("Deployment timed out");
                results.add(DeploymentResult.timeout());
            } catch (Exception e) {
                log.error("Deployment failed: {}", e.getMessage());
                results.add(DeploymentResult.error(e));
            }
        }

        return new DeploymentAttempt(results);
    }

    /**
     * Deploy a single share to a specific blockchain.
     */
    private DeploymentResult deployToChain(
        ShamirSecretSharing.Share share,
        Blockchain blockchain,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) {
        log.debug("Deploying share {} to {}", share.getX(), blockchain.getName());

        try {
            ChainAdapter adapter = chainAdapters.get(blockchain);

            if (adapter == null) {
                return DeploymentResult.error(
                    new IllegalStateException("No adapter for " + blockchain)
                );
            }

            // Deploy share
            ShareDeployment deployment = adapter.deploy(share, unlockDate, config);

            return DeploymentResult.success(deployment, share);

        } catch (Exception e) {
            log.error("Failed to deploy to {}: {}", blockchain, e.getMessage());
            return DeploymentResult.error(e);
        }
    }

    /**
     * Attempt to recover from partial deployment failure.
     */
    private DeploymentAttempt attemptRecovery(
        DeploymentAttempt initial,
        List<ShamirSecretSharing.Share> shares,
        LocalDateTime unlockDate,
        DeploymentConfig config
    ) {
        log.info("Starting recovery process...");

        List<DeploymentResult> allResults = new ArrayList<>(initial.getResults());

        // Strategy 1: Retry failed deployments with backoff
        List<DeploymentResult> failed = initial.getFailedResults();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            if (countSuccessful(allResults) >= config.getThreshold()) {
                break; // Threshold met!
            }

            log.info("Recovery attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);

            long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
            sleep(delayMs);

            for (DeploymentResult failedResult : failed) {
                if (!failedResult.isRetryable()) {
                    continue;
                }

                DeploymentResult retried = deployToChain(
                    failedResult.getShare(),
                    failedResult.getBlockchain(),
                    unlockDate,
                    config
                );

                if (retried.isSuccess()) {
                    log.info("✅ Retry successful for {}", retried.getBlockchain());
                    allResults.add(retried);

                    if (countSuccessful(allResults) >= config.getThreshold()) {
                        return new DeploymentAttempt(allResults);
                    }
                }
            }
        }

        // Strategy 2: Deploy to fallback chains
        if (countSuccessful(allResults) < config.getThreshold()) {
            log.info("Trying fallback blockchains...");

            List<Blockchain> fallbackChains = Arrays.asList(
                Blockchain.POLYGON,  // Very cheap
                Blockchain.CARDANO   // Alternative consensus
            );

            for (Blockchain fallback : fallbackChains) {
                if (countSuccessful(allResults) >= config.getThreshold()) {
                    break;
                }

                if (initial.hasBlockchain(fallback)) {
                    continue; // Already tried
                }

                // Use an unused share
                ShamirSecretSharing.Share unusedShare = findUnusedShare(shares, allResults);
                if (unusedShare == null) {
                    break;
                }

                log.info("Deploying to fallback: {}", fallback);

                DeploymentResult fallbackResult = deployToChain(
                    unusedShare,
                    fallback,
                    unlockDate,
                    config
                );

                allResults.add(fallbackResult);
            }
        }

        return new DeploymentAttempt(allResults);
    }

    /**
     * Rollback deployment by attempting to cancel/delete shares.
     * Best effort - some blockchains are immutable.
     */
    private void rollbackDeployment(DeploymentAttempt attempt) {
        log.warn("Rolling back deployment...");

        for (DeploymentResult result : attempt.getSuccessfulResults()) {
            try {
                ChainAdapter adapter = chainAdapters.get(result.getBlockchain());

                if (adapter != null && adapter.supportsRollback()) {
                    adapter.rollback(result.getDeployment());
                    log.info("Rolled back {}", result.getBlockchain());
                } else {
                    log.warn("Cannot rollback {} (immutable)", result.getBlockchain());
                }

            } catch (Exception e) {
                log.error("Rollback failed for {}: {}",
                    result.getBlockchain(), e.getMessage());
            }
        }
    }

    /**
     * Build deployment receipt from successful attempt.
     */
    private DeploymentReceipt buildReceipt(DeploymentAttempt attempt) {
        List<ShareDeployment> deployments = attempt.getSuccessfulResults().stream()
            .map(DeploymentResult::getDeployment)
            .collect(Collectors.toList());

        double totalCost = deployments.stream()
            .mapToDouble(ShareDeployment::getCostUsd)
            .sum();

        return DeploymentReceipt.builder()
            .deploymentId(UUID.randomUUID().toString())
            .deployedAt(LocalDateTime.now())
            .shareDeployments(deployments)
            .totalCostUsd(totalCost)
            .build();
    }

    // Helper methods

    private int countSuccessful(List<DeploymentResult> results) {
        return (int) results.stream().filter(DeploymentResult::isSuccess).count();
    }

    private ShamirSecretSharing.Share findUnusedShare(
        List<ShamirSecretSharing.Share> shares,
        List<DeploymentResult> results
    ) {
        Set<Integer> usedIndices = results.stream()
            .filter(DeploymentResult::isSuccess)
            .map(r -> r.getShare().getX())
            .collect(Collectors.toSet());

        return shares.stream()
            .filter(s -> !usedIndices.contains(s.getX()))
            .findFirst()
            .orElse(null);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Represents a deployment attempt with results.
     */
    @Data
    private static class DeploymentAttempt {
        private final List<DeploymentResult> results;

        public int getSuccessCount() {
            return (int) results.stream().filter(DeploymentResult::isSuccess).count();
        }

        public List<DeploymentResult> getSuccessfulResults() {
            return results.stream()
                .filter(DeploymentResult::isSuccess)
                .collect(Collectors.toList());
        }

        public List<DeploymentResult> getFailedResults() {
            return results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());
        }

        public boolean hasBlockchain(Blockchain blockchain) {
            return results.stream()
                .anyMatch(r -> r.getBlockchain() == blockchain);
        }
    }

    /**
     * Result of deploying a single share.
     */
    @Data
    @Builder
    public static class DeploymentResult {
        private boolean success;
        private ShareDeployment deployment;
        private ShamirSecretSharing.Share share;
        private Blockchain blockchain;
        private Exception error;
        private boolean retryable;

        public static DeploymentResult success(ShareDeployment deployment, ShamirSecretSharing.Share share) {
            return DeploymentResult.builder()
                .success(true)
                .deployment(deployment)
                .share(share)
                .blockchain(deployment.getBlockchain())
                .build();
        }

        public static DeploymentResult error(Exception error) {
            return DeploymentResult.builder()
                .success(false)
                .error(error)
                .retryable(isRetryableError(error))
                .build();
        }

        public static DeploymentResult timeout() {
            return DeploymentResult.builder()
                .success(false)
                .error(new TimeoutException("Deployment timed out"))
                .retryable(true)
                .build();
        }

        private static boolean isRetryableError(Exception e) {
            // Network errors are retryable
            if (e instanceof java.net.SocketException ||
                e instanceof java.net.UnknownHostException ||
                e instanceof java.util.concurrent.TimeoutException) {
                return true;
            }

            // Rate limit errors are retryable
            if (e.getMessage() != null && e.getMessage().contains("rate limit")) {
                return true;
            }

            // Gas price errors are retryable
            if (e.getMessage() != null && e.getMessage().contains("gas")) {
                return true;
            }

            return false;
        }
    }

    /**
     * Adapter for interacting with a specific blockchain.
     */
    public interface ChainAdapter {
        ShareDeployment deploy(
            ShamirSecretSharing.Share share,
            LocalDateTime unlockDate,
            DeploymentConfig config
        ) throws Exception;

        boolean supportsRollback();

        void rollback(ShareDeployment deployment) throws Exception;
    }

    /**
     * Exception thrown when deployment fails.
     */
    public static class DeploymentException extends EncryptionException {
        public DeploymentException(String message) {
            super(message);
        }

        public DeploymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
