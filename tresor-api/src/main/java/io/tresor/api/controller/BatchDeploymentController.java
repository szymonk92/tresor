package io.tresor.api.controller;

import io.tresor.api.service.BatchDeploymentService;
import io.tresor.api.service.BatchDeploymentService.BatchDeploymentResult;
import io.tresor.api.service.BatchDeploymentService.BatchDeploymentStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for batch deployment management.
 *
 * Endpoints:
 * - GET  /api/batch/stats    - Get batch deployment statistics
 * - POST /api/batch/trigger  - Manually trigger batch deployment (admin)
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
public class BatchDeploymentController {

    private final BatchDeploymentService batchDeploymentService;

    public BatchDeploymentController(BatchDeploymentService batchDeploymentService) {
        this.batchDeploymentService = batchDeploymentService;
    }

    /**
     * Get batch deployment statistics.
     *
     * GET /api/batch/stats
     *
     * Response:
     * {
     *   "pendingMessages": 42,
     *   "pendingByTier": {
     *     "budget": 35,
     *     "standard": 5,
     *     "premium": 2
     *   },
     *   "estimatedCost": 24.50,
     *   "timestamp": "2025-01-15T10:30:00"
     * }
     */
    @GetMapping("/stats")
    public ResponseEntity<BatchDeploymentStats> getStats() {
        try {
            BatchDeploymentStats stats = batchDeploymentService.getStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get batch stats: {}", e.getMessage());
            return ResponseEntity
                .internalServerError()
                .build();
        }
    }

    /**
     * Manually trigger batch deployment.
     *
     * POST /api/batch/trigger
     *
     * This endpoint is useful for:
     * - Testing batch deployment without waiting for cron
     * - Admin operations to force deployment
     * - Debugging deployment issues
     *
     * Response:
     * {
     *   "deployed": 35,
     *   "failed": 2,
     *   "totalCost": 20.50,
     *   "summary": "Deployed 35 messages, 2 failed, total cost: $20.50"
     * }
     */
    @PostMapping("/trigger")
    public ResponseEntity<BatchDeploymentResult> triggerBatchDeployment(
        @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        try {
            // TODO: Validate admin key in production
            // For MVP, allow anyone to trigger (useful for testing)

            log.info("Manual batch deployment triggered by admin");

            BatchDeploymentResult result = batchDeploymentService.triggerBatchDeployment();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to trigger batch deployment: {}", e.getMessage());
            return ResponseEntity
                .internalServerError()
                .build();
        }
    }

    /**
     * Health check for batch service.
     *
     * GET /api/batch/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        BatchDeploymentStats stats = batchDeploymentService.getStats();

        return ResponseEntity.ok(new HealthResponse(
            "ok",
            "Batch deployment service is running",
            stats.pendingMessages(),
            stats.timestamp()
        ));
    }

    public record HealthResponse(
        String status,
        String message,
        long pendingMessages,
        java.time.LocalDateTime timestamp
    ) {}
}
