package io.tresor.api.service;

import io.tresor.api.model.Message;
import io.tresor.api.model.Message.MessageStatus;
import io.tresor.api.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for BatchDeploymentService.
 */
class BatchDeploymentServiceIntegrationTest {

    private BatchDeploymentService batchDeploymentService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchDeploymentService = new BatchDeploymentService(messageRepository, messageService);
    }

    @Test
    @DisplayName("Should get batch deployment statistics")
    void testGetStats() {
        // Given
        Message msg1 = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message msg2 = createMessage("msg-2", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message msg3 = createMessage("msg-3", "standard", MessageStatus.PENDING_BATCH, 3.00);

        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(Arrays.asList(msg1, msg2, msg3));

        // When
        BatchDeploymentService.BatchDeploymentStats stats = batchDeploymentService.getStats();

        // Then
        assertNotNull(stats);
        assertEquals(3, stats.pendingMessages());
        assertEquals(4.00, stats.estimatedCost(), 0.01);
        assertEquals(2, stats.pendingByTier().get("budget"));
        assertEquals(1, stats.pendingByTier().get("standard"));
    }

    @Test
    @DisplayName("Should trigger batch deployment successfully")
    void testTriggerBatchDeployment() {
        // Given
        Message msg1 = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message msg2 = createMessage("msg-2", "budget", MessageStatus.PENDING_BATCH, 0.50);

        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(Arrays.asList(msg1, msg2));

        // When
        BatchDeploymentService.BatchDeploymentResult result = batchDeploymentService.triggerBatchDeployment();

        // Then
        assertNotNull(result);
        assertEquals(2, result.deployed());
        assertEquals(0, result.failed());
        assertEquals(1.00, result.totalCost(), 0.01);
        verify(messageRepository, atLeast(2)).save(any(Message.class));
    }

    @Test
    @DisplayName("Should handle empty pending messages")
    void testEmptyPendingMessages() {
        // Given
        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(List.of());

        // When
        BatchDeploymentService.BatchDeploymentResult result = batchDeploymentService.triggerBatchDeployment();

        // Then
        assertNotNull(result);
        assertEquals(0, result.deployed());
        assertEquals(0, result.failed());
        assertTrue(result.summary().contains("No messages"));
    }

    @Test
    @DisplayName("Should group messages by tier")
    void testGroupMessagesByTier() {
        // Given
        Message budgetMsg1 = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message budgetMsg2 = createMessage("msg-2", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message premiumMsg = createMessage("msg-3", "premium", MessageStatus.PENDING_BATCH, 13.00);
        Message enterpriseMsg = createMessage("msg-4", "enterprise", MessageStatus.PENDING_BATCH, 25.00);

        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(Arrays.asList(budgetMsg1, budgetMsg2, premiumMsg, enterpriseMsg));

        // When
        BatchDeploymentService.BatchDeploymentStats stats = batchDeploymentService.getStats();

        // Then
        assertEquals(2, stats.pendingByTier().get("budget"));
        assertEquals(1, stats.pendingByTier().get("premium"));
        assertEquals(1, stats.pendingByTier().get("enterprise"));
    }

    @Test
    @DisplayName("Should calculate total estimated cost correctly")
    void testEstimatedCostCalculation() {
        // Given
        Message msg1 = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message msg2 = createMessage("msg-2", "premium", MessageStatus.PENDING_BATCH, 13.00);
        Message msg3 = createMessage("msg-3", "enterprise", MessageStatus.PENDING_BATCH, 25.00);

        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(Arrays.asList(msg1, msg2, msg3));

        // When
        BatchDeploymentService.BatchDeploymentStats stats = batchDeploymentService.getStats();

        // Then
        assertEquals(38.50, stats.estimatedCost(), 0.01);
    }

    @Test
    @DisplayName("Should update message status during deployment")
    void testStatusTransitionDuringDeployment() {
        // Given
        Message message = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(List.of(message));

        // When
        batchDeploymentService.triggerBatchDeployment();

        // Then
        verify(messageRepository, atLeastOnce()).save(argThat(msg ->
            msg.getStatus() == MessageStatus.DEPLOYING ||
            msg.getStatus() == MessageStatus.LOCKED
        ));
    }

    @Test
    @DisplayName("Should handle deployment failures gracefully")
    void testDeploymentFailureHandling() {
        // Given
        Message message = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(List.of(message));
        doThrow(new RuntimeException("Deployment failed"))
            .when(messageRepository).save(any(Message.class));

        // When
        BatchDeploymentService.BatchDeploymentResult result = batchDeploymentService.triggerBatchDeployment();

        // Then
        assertNotNull(result);
        // Should not throw exception, but handle gracefully
    }

    @Test
    @DisplayName("Should only process PENDING_BATCH messages")
    void testOnlyProcessPendingBatch() {
        // Given
        Message pendingMsg = createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50);
        Message lockedMsg = createMessage("msg-2", "budget", MessageStatus.LOCKED, 0.50);

        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(List.of(pendingMsg));

        // When
        BatchDeploymentService.BatchDeploymentResult result = batchDeploymentService.triggerBatchDeployment();

        // Then
        assertEquals(1, result.deployed()); // Only pending message deployed
    }

    @Test
    @DisplayName("Should handle multiple tiers in single batch")
    void testMultipleTiersInBatch() {
        // Given
        List<Message> messages = Arrays.asList(
            createMessage("msg-1", "budget", MessageStatus.PENDING_BATCH, 0.50),
            createMessage("msg-2", "budget", MessageStatus.PENDING_BATCH, 0.50),
            createMessage("msg-3", "standard", MessageStatus.PENDING_BATCH, 3.00),
            createMessage("msg-4", "premium", MessageStatus.PENDING_BATCH, 13.00),
            createMessage("msg-5", "enterprise", MessageStatus.PENDING_BATCH, 25.00)
        );

        when(messageRepository.findByStatus(MessageStatus.PENDING_BATCH))
            .thenReturn(messages);

        // When
        BatchDeploymentService.BatchDeploymentResult result = batchDeploymentService.triggerBatchDeployment();

        // Then
        assertEquals(5, result.deployed());
        assertEquals(42.00, result.totalCost(), 0.01);
    }

    // Helper method to create test messages
    private Message createMessage(String id, String tier, MessageStatus status, double cost) {
        Message message = new Message();
        message.setId(id);
        message.setDeploymentTier(tier);
        message.setStatus(status);
        message.setCostUsd(cost);
        message.setCreatedAt(LocalDateTime.now());
        message.setUnlockDate(LocalDateTime.now().plusYears(1));
        message.setEncryptionMode("PASSWORD_ENCRYPTION");
        return message;
    }
}
