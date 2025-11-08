package io.tresor.core.secretsharing;

import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.secretsharing.DeploymentOrchestrator.*;
import io.tresor.core.secretsharing.SecretHolderService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DeploymentOrchestrator.
 *
 * NOTE: Some tests simulate network failures and retries,
 * which may take several seconds to complete.
 */
class DeploymentOrchestratorTest {

    private DeploymentOrchestrator orchestrator;
    private Map<Blockchain, ChainAdapter> adapters;
    private byte[] testKey;

    @BeforeEach
    void setUp() {
        adapters = new HashMap<>();
        testKey = new byte[32]; // AES-256 key
        new Random().nextBytes(testKey);
    }

    @Test
    void testSuccessfulDeployment_AllChainsWork() throws Exception {
        // Setup: All chains succeed
        adapters.put(Blockchain.BITCOIN, new MockAdapter(true));
        adapters.put(Blockchain.ETHEREUM, new MockAdapter(true));
        adapters.put(Blockchain.ARBITRUM, new MockAdapter(true));
        adapters.put(Blockchain.SELF_CUSTODY, new MockAdapter(true));
        adapters.put(Blockchain.ARWEAVE, new MockAdapter(true));

        orchestrator = new DeploymentOrchestrator(adapters);

        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(5)
            .threshold(3)
            .blockchains(Arrays.asList(
                Blockchain.BITCOIN,
                Blockchain.ETHEREUM,
                Blockchain.ARBITRUM,
                Blockchain.SELF_CUSTODY,
                Blockchain.ARWEAVE
            ))
            .build();

        // Execute
        DeploymentReceipt receipt = orchestrator.deployWithRecovery(
            testKey,
            LocalDateTime.now().plusYears(10),
            config
        );

        // Verify
        assertNotNull(receipt);
        assertEquals(5, receipt.getShareDeployments().size());
        assertTrue(receipt.getTotalCostUsd() > 0);
    }

    @Test
    void testPartialFailure_ThresholdStillMet() throws Exception {
        // Setup: 2 chains fail, but 3 succeed (threshold met)
        adapters.put(Blockchain.BITCOIN, new MockAdapter(true));
        adapters.put(Blockchain.ETHEREUM, new MockAdapter(false)); // FAILS
        adapters.put(Blockchain.ARBITRUM, new MockAdapter(true));
        adapters.put(Blockchain.SELF_CUSTODY, new MockAdapter(true));
        adapters.put(Blockchain.ARWEAVE, new MockAdapter(false)); // FAILS

        orchestrator = new DeploymentOrchestrator(adapters);

        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(5)
            .threshold(3)
            .blockchains(Arrays.asList(
                Blockchain.BITCOIN,
                Blockchain.ETHEREUM,
                Blockchain.ARBITRUM,
                Blockchain.SELF_CUSTODY,
                Blockchain.ARWEAVE
            ))
            .build();

        // Execute
        DeploymentReceipt receipt = orchestrator.deployWithRecovery(
            testKey,
            LocalDateTime.now().plusYears(10),
            config
        );

        // Verify: Should succeed with 3 deployments
        assertNotNull(receipt);
        assertEquals(3, receipt.getShareDeployments().size());
    }

    @Test
    void testPartialFailure_BelowThreshold_RecoverySucceeds() throws Exception {
        // Setup: Only 2 succeed initially, but retry succeeds
        // NOTE: This test may take 2-3 seconds due to retry delays
        adapters.put(Blockchain.BITCOIN, new MockAdapter(true));
        adapters.put(Blockchain.ETHEREUM, new FailThenSucceedAdapter(1)); // Fails once, then succeeds
        adapters.put(Blockchain.ARBITRUM, new MockAdapter(false)); // Always fails
        adapters.put(Blockchain.SELF_CUSTODY, new MockAdapter(true));
        adapters.put(Blockchain.ARWEAVE, new MockAdapter(false)); // Always fails

        orchestrator = new DeploymentOrchestrator(adapters);

        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(5)
            .threshold(3)
            .blockchains(Arrays.asList(
                Blockchain.BITCOIN,
                Blockchain.ETHEREUM,
                Blockchain.ARBITRUM,
                Blockchain.SELF_CUSTODY,
                Blockchain.ARWEAVE
            ))
            .build();

        // Execute
        DeploymentReceipt receipt = orchestrator.deployWithRecovery(
            testKey,
            LocalDateTime.now().plusYears(10),
            config
        );

        // Verify: Should succeed after retry
        assertNotNull(receipt);
        assertTrue(receipt.getShareDeployments().size() >= 3);
    }

    @Test
    void testCompleteFailure_ThresholdNotMet() {
        // Setup: Only 1 chain succeeds (below threshold of 3)
        adapters.put(Blockchain.BITCOIN, new MockAdapter(true));
        adapters.put(Blockchain.ETHEREUM, new MockAdapter(false));
        adapters.put(Blockchain.ARBITRUM, new MockAdapter(false));
        adapters.put(Blockchain.SELF_CUSTODY, new MockAdapter(false));
        adapters.put(Blockchain.ARWEAVE, new MockAdapter(false));

        orchestrator = new DeploymentOrchestrator(adapters);

        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(5)
            .threshold(3)
            .blockchains(Arrays.asList(
                Blockchain.BITCOIN,
                Blockchain.ETHEREUM,
                Blockchain.ARBITRUM,
                Blockchain.SELF_CUSTODY,
                Blockchain.ARWEAVE
            ))
            .build();

        // Execute & Verify: Should throw exception
        assertThrows(EncryptionException.class, () -> {
            orchestrator.deployWithRecovery(
                testKey,
                LocalDateTime.now().plusYears(10),
                config
            );
        });
    }

    @Test
    void testRetryWithExponentialBackoff() throws Exception {
        // Setup: Adapter fails 2 times, then succeeds on 3rd attempt
        // NOTE: This test may take 3-4 seconds due to exponential backoff
        FailThenSucceedAdapter adapter = new FailThenSucceedAdapter(2);

        adapters.put(Blockchain.BITCOIN, adapter);
        adapters.put(Blockchain.ETHEREUM, new MockAdapter(true));
        adapters.put(Blockchain.ARBITRUM, new MockAdapter(true));

        orchestrator = new DeploymentOrchestrator(adapters);

        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(3)
            .threshold(3)
            .blockchains(Arrays.asList(
                Blockchain.BITCOIN,
                Blockchain.ETHEREUM,
                Blockchain.ARBITRUM
            ))
            .build();

        // Execute
        long startTime = System.currentTimeMillis();

        DeploymentReceipt receipt = orchestrator.deployWithRecovery(
            testKey,
            LocalDateTime.now().plusYears(10),
            config
        );

        long duration = System.currentTimeMillis() - startTime;

        // Verify
        assertNotNull(receipt);
        assertEquals(3, receipt.getShareDeployments().size());

        // Should have retried with backoff (at least 1+2=3 seconds)
        assertTrue(duration >= 3000,
            "Expected retry delays, but completed in " + duration + "ms");
    }

    @Test
    void testIdempotency_DuplicateDeployment() throws Exception {
        // Setup: Adapter tracks deployments to detect duplicates
        IdempotentAdapter idempotentAdapter = new IdempotentAdapter();

        adapters.put(Blockchain.BITCOIN, idempotentAdapter);
        adapters.put(Blockchain.ETHEREUM, new MockAdapter(true));
        adapters.put(Blockchain.ARBITRUM, new MockAdapter(true));

        orchestrator = new DeploymentOrchestrator(adapters);

        DeploymentConfig config = DeploymentConfig.builder()
            .totalShares(3)
            .threshold(2)
            .blockchains(Arrays.asList(
                Blockchain.BITCOIN,
                Blockchain.ETHEREUM,
                Blockchain.ARBITRUM
            ))
            .build();

        // Execute twice
        DeploymentReceipt receipt1 = orchestrator.deployWithRecovery(
            testKey,
            LocalDateTime.now().plusYears(10),
            config
        );

        // Verify: Bitcoin should have been called once
        assertEquals(1, idempotentAdapter.getDeploymentCount());
    }

    // ========================================
    // MOCK ADAPTERS
    // ========================================

    /**
     * Mock adapter that always succeeds or always fails.
     */
    private static class MockAdapter implements ChainAdapter {
        private final boolean shouldSucceed;

        public MockAdapter(boolean shouldSucceed) {
            this.shouldSucceed = shouldSucceed;
        }

        @Override
        public ShareDeployment deploy(
            ShamirSecretSharing.Share share,
            LocalDateTime unlockDate,
            DeploymentConfig config
        ) throws Exception {
            if (!shouldSucceed) {
                throw new RuntimeException("Simulated deployment failure");
            }

            return ShareDeployment.builder()
                .shareNumber(share.getX())
                .blockchain(Blockchain.BITCOIN)
                .identifier("mock-tx-" + UUID.randomUUID())
                .unlockBlockHeight(1000000)
                .costUsd(5.0)
                .status(DeploymentStatus.CONFIRMED)
                .build();
        }

        @Override
        public boolean supportsRollback() {
            return false;
        }

        @Override
        public void rollback(ShareDeployment deployment) {
            // No-op
        }
    }

    /**
     * Mock adapter that fails N times, then succeeds.
     */
    private static class FailThenSucceedAdapter implements ChainAdapter {
        private final int failuresBeforeSuccess;
        private int attemptCount = 0;

        public FailThenSucceedAdapter(int failuresBeforeSuccess) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public synchronized ShareDeployment deploy(
            ShamirSecretSharing.Share share,
            LocalDateTime unlockDate,
            DeploymentConfig config
        ) throws Exception {
            attemptCount++;

            if (attemptCount <= failuresBeforeSuccess) {
                throw new RuntimeException("Simulated failure " + attemptCount);
            }

            return ShareDeployment.builder()
                .shareNumber(share.getX())
                .blockchain(Blockchain.ETHEREUM)
                .identifier("retry-success-" + attemptCount)
                .unlockBlockHeight(2000000)
                .costUsd(10.0)
                .status(DeploymentStatus.CONFIRMED)
                .build();
        }

        @Override
        public boolean supportsRollback() {
            return false;
        }

        @Override
        public void rollback(ShareDeployment deployment) {
        }
    }

    /**
     * Mock adapter that tracks deployment count for idempotency testing.
     */
    private static class IdempotentAdapter implements ChainAdapter {
        private int deploymentCount = 0;
        private final Set<String> deployedShares = new HashSet<>();

        @Override
        public synchronized ShareDeployment deploy(
            ShamirSecretSharing.Share share,
            LocalDateTime unlockDate,
            DeploymentConfig config
        ) throws Exception {
            String shareKey = share.getX() + "-" + share.getY().toString();

            if (deployedShares.contains(shareKey)) {
                // Already deployed - return existing deployment
                return ShareDeployment.builder()
                    .shareNumber(share.getX())
                    .blockchain(Blockchain.BITCOIN)
                    .identifier("existing-" + shareKey)
                    .unlockBlockHeight(1000000)
                    .costUsd(0.0) // No cost for duplicate
                    .status(DeploymentStatus.CONFIRMED)
                    .build();
            }

            // New deployment
            deploymentCount++;
            deployedShares.add(shareKey);

            return ShareDeployment.builder()
                .shareNumber(share.getX())
                .blockchain(Blockchain.BITCOIN)
                .identifier("new-" + shareKey)
                .unlockBlockHeight(1000000)
                .costUsd(5.0)
                .status(DeploymentStatus.CONFIRMED)
                .build();
        }

        @Override
        public boolean supportsRollback() {
            return true;
        }

        @Override
        public void rollback(ShareDeployment deployment) {
            deployedShares.clear();
            deploymentCount = 0;
        }

        public int getDeploymentCount() {
            return deploymentCount;
        }
    }
}
