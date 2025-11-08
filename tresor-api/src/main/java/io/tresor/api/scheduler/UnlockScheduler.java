package io.tresor.api.scheduler;

import io.tresor.api.service.ShareRetrievalService;
import io.tresor.api.service.UnlockService;
import io.tresor.api.service.UnlockService.UnlockResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled task for checking and unlocking messages.
 *
 * Runs every 10 minutes to check for messages that should be unlocked.
 *
 * Strategy:
 * 1. Find messages with unlock_date <= now AND status = LOCKED
 * 2. Try to unlock each message
 * 3. If insufficient shares: Schedule retry with backoff
 * 4. Track retry attempts to prevent infinite loops
 *
 * Retry schedule:
 * - 0-1 hour: Every 5 minutes
 * - 1-6 hours: Every 30 minutes
 * - 6-24 hours: Every 2 hours
 * - 24+ hours: Every 6 hours
 * - 7+ days: Notify user of issue
 */
@Slf4j
@Service
public class UnlockScheduler {

    private final UnlockService unlockService;
    private final MessageRepository messageRepository;
    private final ShareRetrievalService shareRetrievalService;
    private final NotificationService notificationService;

    // Track retry attempts for each message
    private final Map<String, RetryState> retryStates = new ConcurrentHashMap<>();

    public UnlockScheduler(
        UnlockService unlockService,
        MessageRepository messageRepository,
        ShareRetrievalService shareRetrievalService,
        NotificationService notificationService
    ) {
        this.unlockService = unlockService;
        this.messageRepository = messageRepository;
        this.shareRetrievalService = shareRetrievalService;
        this.notificationService = notificationService;
    }

    /**
     * Check for unlockable messages every 10 minutes.
     */
    @Scheduled(cron = "0 */10 * * * *") // Every 10 minutes
    public void checkUnlockableMessages() {
        log.info("Checking for unlockable messages...");

        // Find messages that should be unlocked
        List<Message> unlockable = messageRepository.findUnlockable(LocalDateTime.now());

        log.info("Found {} messages ready to unlock", unlockable.size());

        for (Message message : unlockable) {
            processMessage(message);
        }

        // Cleanup old retry states
        cleanupRetryStates();
    }

    /**
     * Process a single message.
     */
    private void processMessage(Message message) {
        String messageId = message.getId();
        RetryState state = retryStates.getOrDefault(messageId, new RetryState());

        try {
            log.info("Attempting to unlock message: {}", messageId);

            // Try to unlock
            UnlockResult result = unlockService.unlockMessage(messageId);

            if (result.isSuccess()) {
                log.info("✅ Successfully unlocked message: {}", messageId);

                // Remove from retry tracking
                retryStates.remove(messageId);

                // Update retry state
                state.incrementAttempts();
                retryStates.put(messageId, state);
            }

        } catch (ShareRetrievalService.InsufficientSharesException e) {
            // Not enough shares available - schedule retry
            log.warn("Insufficient shares for {}: {}", messageId, e.getMessage());

            state.incrementAttempts();
            retryStates.put(messageId, state);

            scheduleRetry(message, state);

        } catch (Exception e) {
            log.error("Failed to unlock {}: {}", messageId, e.getMessage());

            state.incrementAttempts();
            state.setLastError(e.getMessage());
            retryStates.put(messageId, state);

            handleUnlockFailure(message, state);
        }
    }

    /**
     * Schedule retry based on how long we've been trying.
     */
    private void scheduleRetry(Message message, RetryState state) {
        Duration sinceFirstAttempt = Duration.between(
            state.getFirstAttemptAt(),
            LocalDateTime.now()
        );

        Duration nextRetry = calculateNextRetryDelay(sinceFirstAttempt, state.getAttempts());

        log.info("Scheduling retry for {} in {}", message.getId(), nextRetry);

        // Update next check time
        message.setNextUnlockCheck(LocalDateTime.now().plus(nextRetry));
        messageRepository.save(message);

        // Notify user if taking too long
        if (sinceFirstAttempt.toHours() >= 24 && state.getAttempts() % 4 == 0) {
            notifyUserOfDelay(message, sinceFirstAttempt);
        }

        // Give up after 7 days
        if (sinceFirstAttempt.toDays() >= 7) {
            log.error("Giving up on message {} after 7 days", message.getId());
            notifyUserOfFailure(message);
            retryStates.remove(message.getId());
        }
    }

    /**
     * Calculate next retry delay based on elapsed time.
     */
    private Duration calculateNextRetryDelay(Duration elapsed, int attempts) {
        long hours = elapsed.toHours();

        if (hours < 1) {
            // First hour: Every 5 minutes
            return Duration.ofMinutes(5);
        } else if (hours < 6) {
            // 1-6 hours: Every 30 minutes
            return Duration.ofMinutes(30);
        } else if (hours < 24) {
            // 6-24 hours: Every 2 hours
            return Duration.ofHours(2);
        } else {
            // 24+ hours: Every 6 hours
            return Duration.ofHours(6);
        }
    }

    /**
     * Handle unlock failure after multiple attempts.
     */
    private void handleUnlockFailure(Message message, RetryState state) {
        if (state.getAttempts() >= 3) {
            log.error("Message {} failed {} times: {}",
                message.getId(), state.getAttempts(), state.getLastError());
        }

        if (state.getAttempts() >= 10) {
            log.error("Too many failures for {}, notifying user", message.getId());
            notifyUserOfFailure(message);
        }
    }

    /**
     * Cleanup retry states for messages that have been unlocked or deleted.
     */
    private void cleanupRetryStates() {
        retryStates.entrySet().removeIf(entry -> {
            String messageId = entry.getKey();
            RetryState state = entry.getValue();

            // Remove if older than 30 days
            Duration age = Duration.between(state.getFirstAttemptAt(), LocalDateTime.now());
            if (age.toDays() > 30) {
                log.info("Removing old retry state for {}", messageId);
                return true;
            }

            // Remove if message no longer exists or is unlocked
            Message message = messageRepository.findById(messageId);
            if (message == null || message.getStatus() == MessageStatus.UNLOCKED) {
                return true;
            }

            return false;
        });
    }

    /**
     * Notify user that unlock is delayed.
     */
    private void notifyUserOfDelay(Message message, Duration delay) {
        log.info("Notifying user of delay for message {}: {}", message.getId(), delay);

        notificationService.send(
            message.getUserId(),
            "Your message is taking longer than expected to unlock",
            String.format(
                "We're still working on unlocking your message. " +
                "Some blockchain services are experiencing delays. " +
                "Elapsed time: %d hours. We'll keep trying.",
                delay.toHours()
            )
        );
    }

    /**
     * Notify user that unlock failed permanently.
     */
    private void notifyUserOfFailure(Message message) {
        log.error("Notifying user of permanent failure for {}", message.getId());

        notificationService.send(
            message.getUserId(),
            "Unable to unlock your message",
            "We've been unable to unlock your message after multiple attempts. " +
            "Please contact support with message ID: " + message.getId()
        );
    }

    // ========================================
    // VALUE OBJECTS
    // ========================================

    @Data
    static class RetryState {
        private int attempts = 0;
        private LocalDateTime firstAttemptAt = LocalDateTime.now();
        private LocalDateTime lastAttemptAt;
        private String lastError;

        public void incrementAttempts() {
            this.attempts++;
            this.lastAttemptAt = LocalDateTime.now();
        }
    }

    @Data
    static class Message {
        private String id;
        private String userId;
        private LocalDateTime unlockDate;
        private MessageStatus status;
        private LocalDateTime nextUnlockCheck;
    }

    enum MessageStatus {
        LOCKED,
        UNLOCKING,
        UNLOCKED,
        FAILED
    }

    interface MessageRepository {
        List<Message> findUnlockable(LocalDateTime now);
        Message findById(String id);
        void save(Message message);
    }

    interface NotificationService {
        void send(String userId, String subject, String message);
    }
}
