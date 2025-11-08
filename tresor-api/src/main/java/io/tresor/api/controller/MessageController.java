package io.tresor.api.controller;

import io.tresor.api.model.Message;
import io.tresor.api.model.Message.MessageStatus;
import io.tresor.api.service.MessageService;
import io.tresor.api.service.MessageService.MessageStats;
import io.tresor.core.encryption.EncryptionException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API controller for time-locked messages.
 *
 * Endpoints:
 * - POST   /api/messages          - Create new message
 * - GET    /api/messages          - List user's messages
 * - GET    /api/messages/{id}     - Get specific message
 * - GET    /api/messages/stats    - Get user statistics
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Create a new time-locked message.
     *
     * POST /api/messages
     * {
     *   "textContent": "Dear future self...",
     *   "unlockDate": "2035-01-15T10:00:00",
     *   "encryptionMode": "PASSWORD_ENCRYPTION",  // Optional: FULL_ENCRYPTION, PASSWORD_ENCRYPTION, NO_ENCRYPTION
     *   "password": "mysecretpassword",          // Required for PASSWORD_ENCRYPTION
     *   "passwordHint": "My dog's name",         // Optional hint
     *   "deploymentTier": "budget"               // Optional: budget, standard, premium, enterprise
     * }
     */
    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
        @RequestHeader("X-User-Id") String userId,
        @RequestBody CreateMessageRequest request
    ) {
        try {
            log.info("Creating message for user: {}, mode: {}, tier: {}",
                userId, request.getEncryptionMode(), request.getDeploymentTier());

            Message message = messageService.createMessage(
                userId,
                request.getTextContent(),
                request.getUnlockDate(),
                request.getEncryptionMode(),
                request.getPassword(),
                request.getPasswordHint(),
                request.getDeploymentTier()
            );

            MessageResponse response = MessageResponse.fromMessage(message);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.error(e.getMessage()));

        } catch (EncryptionException e) {
            log.error("Encryption failed: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponse.error("Encryption failed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * Get all messages for user.
     *
     * GET /api/messages?status=LOCKED
     */
    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
        @RequestHeader("X-User-Id") String userId,
        @RequestParam(required = false) MessageStatus status
    ) {
        try {
            List<Message> messages;

            if (status != null) {
                messages = messageService.getUserMessagesByStatus(userId, status);
            } else {
                messages = messageService.getUserMessages(userId);
            }

            List<MessageResponse> response = messages.stream()
                .map(MessageResponse::fromMessage)
                .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get messages: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Get specific message.
     *
     * GET /api/messages/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMessage(
        @RequestHeader("X-User-Id") String userId,
        @PathVariable String id
    ) {
        try {
            Message message = messageService.getMessage(id, userId);
            MessageResponse response = MessageResponse.fromMessage(message);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(MessageResponse.error("Message not found"));

        } catch (SecurityException e) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.error("Not authorized"));

        } catch (Exception e) {
            log.error("Failed to get message: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Get user statistics.
     *
     * GET /api/messages/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<MessageStats> getStats(
        @RequestHeader("X-User-Id") String userId
    ) {
        try {
            MessageStats stats = messageService.getStats(userId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Health check endpoint.
     *
     * GET /api/messages/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
            "ok",
            "Tresor API is running",
            LocalDateTime.now()
        ));
    }

    // ========================================
    // REQUEST/RESPONSE DTOs
    // ========================================

    @Data
    public static class CreateMessageRequest {
        private String textContent;
        private LocalDateTime unlockDate;

        // Encryption options
        private String encryptionMode;  // "FULL_ENCRYPTION", "PASSWORD_ENCRYPTION", "NO_ENCRYPTION"
        private String password;        // Required for PASSWORD_ENCRYPTION mode
        private String passwordHint;    // Optional hint (NOT the password!)

        // Deployment options
        private String deploymentTier;  // "budget", "standard", "premium", "enterprise"
    }

    @Data
    public static class MessageResponse {
        private boolean success;
        private String error;

        // Message fields
        private String id;
        private String userId;
        private String deliveryEmail;
        private LocalDateTime createdAt;
        private LocalDateTime unlockDate;
        private MessageStatus status;
        private String contentHash;
        private double costUsd;
        private LocalDateTime unlockedAt;
        private LocalDateTime deliveredAt;

        // New fields
        private String encryptionMode;
        private String deploymentTier;
        private String passwordHint;  // For PASSWORD_ENCRYPTION mode

        public static MessageResponse fromMessage(Message message) {
            MessageResponse response = new MessageResponse();
            response.setSuccess(true);
            response.setId(message.getId());
            response.setUserId(message.getUserId());
            response.setDeliveryEmail(message.getDeliveryEmail());
            response.setCreatedAt(message.getCreatedAt());
            response.setUnlockDate(message.getUnlockDate());
            response.setStatus(message.getStatus());
            response.setContentHash(message.getContentHash());
            response.setCostUsd(message.getCostUsd());
            response.setUnlockedAt(message.getUnlockedAt());
            response.setDeliveredAt(message.getDeliveredAt());
            response.setEncryptionMode(message.getEncryptionMode());
            response.setDeploymentTier(message.getDeploymentTier());
            response.setPasswordHint(message.getPasswordHint());
            return response;
        }

        public static MessageResponse error(String error) {
            MessageResponse response = new MessageResponse();
            response.setSuccess(false);
            response.setError(error);
            return response;
        }
    }

    public record HealthResponse(
        String status,
        String message,
        LocalDateTime timestamp
    ) {}
}
