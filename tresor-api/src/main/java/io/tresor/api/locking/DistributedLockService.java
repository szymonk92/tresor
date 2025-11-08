package io.tresor.api.locking;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed lock service to prevent race conditions.
 *
 * Problem solved:
 * - Cron job starts unlocking message A
 * - User clicks "unlock now" simultaneously
 * - Both processes run in parallel → duplicate work, conflicts
 *
 * Solution:
 * - Acquire distributed lock before critical operations
 * - Only one process can hold lock at a time
 * - Lock automatically released after timeout
 *
 * Implementations:
 * - Redis (Redisson)
 * - Database (PostgreSQL advisory locks)
 * - Hazelcast
 * - Zookeeper
 */
@Slf4j
public interface DistributedLockService {

    /**
     * Execute operation with distributed lock.
     *
     * Pattern:
     * ```java
     * lockService.withLock("unlock:msg123", () -> {
     *     unlockMessage("msg123");
     *     return result;
     * });
     * ```
     *
     * @param lockKey Unique key for this lock
     * @param operation Operation to execute while holding lock
     * @param <T> Return type
     * @return Result of operation
     * @throws LockException if lock cannot be acquired
     */
    <T> T withLock(String lockKey, Supplier<T> operation) throws LockException;

    /**
     * Execute operation with lock and custom timeout.
     *
     * @param lockKey Unique key for this lock
     * @param waitTimeout Max time to wait for lock
     * @param leaseTimeout Max time to hold lock (auto-release)
     * @param operation Operation to execute
     * @param <T> Return type
     * @return Result of operation
     * @throws LockException if lock cannot be acquired
     */
    <T> T withLock(
        String lockKey,
        Duration waitTimeout,
        Duration leaseTimeout,
        Supplier<T> operation
    ) throws LockException;

    /**
     * Try to acquire lock without blocking.
     *
     * @param lockKey Unique key
     * @return Lock handle if acquired, null otherwise
     */
    DistributedLock tryLock(String lockKey);

    /**
     * Try to acquire lock with timeout.
     *
     * @param lockKey Unique key
     * @param waitTimeout Max time to wait
     * @param leaseTimeout Max time to hold lock
     * @return Lock handle if acquired, null otherwise
     */
    DistributedLock tryLock(String lockKey, Duration waitTimeout, Duration leaseTimeout);

    /**
     * Check if a lock is currently held.
     *
     * @param lockKey Unique key
     * @return true if locked
     */
    boolean isLocked(String lockKey);

    /**
     * Handle to a distributed lock.
     */
    interface DistributedLock extends AutoCloseable {
        /**
         * Release the lock.
         */
        void unlock();

        /**
         * Check if this lock is still held.
         */
        boolean isHeldByCurrentThread();

        /**
         * Extend the lease time.
         */
        boolean extend(Duration additionalTime);

        @Override
        default void close() {
            unlock();
        }
    }

    /**
     * Exception thrown when lock operations fail.
     */
    class LockException extends Exception {
        public LockException(String message) {
            super(message);
        }

        public LockException(String message, Throwable cause) {
            super(message, cause);
        }

        public static class TimeoutException extends LockException {
            public TimeoutException(String lockKey, Duration timeout) {
                super(String.format("Failed to acquire lock '%s' within %s",
                    lockKey, timeout));
            }
        }

        public static class AlreadyLockedException extends LockException {
            public AlreadyLockedException(String lockKey) {
                super(String.format("Lock '%s' is already held", lockKey));
            }
        }
    }
}
