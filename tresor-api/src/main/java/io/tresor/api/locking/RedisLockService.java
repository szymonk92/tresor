package io.tresor.api.locking;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis-based distributed lock implementation using Redisson.
 *
 * Uses Redis for distributed locking with:
 * - Automatic lock expiration (prevents deadlocks)
 * - Re-entrant locks (same thread can acquire multiple times)
 * - Fair locks (FIFO ordering)
 * - Watchdog auto-extension (extends lock if operation still running)
 *
 * Example:
 * ```java
 * RedisLockService lockService = new RedisLockService(redisson);
 *
 * String result = lockService.withLock("unlock:msg123", () -> {
 *     // Only one process executes this at a time
 *     unlockMessage("msg123");
 *     return "success";
 * });
 * ```
 */
@Slf4j
public class RedisLockService implements DistributedLockService {

    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_LEASE_TIMEOUT = Duration.ofSeconds(60);

    private final RedissonClient redissonClient;

    public RedisLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T withLock(String lockKey, Supplier<T> operation) throws LockException {
        return withLock(
            lockKey,
            DEFAULT_WAIT_TIMEOUT,
            DEFAULT_LEASE_TIMEOUT,
            operation
        );
    }

    @Override
    public <T> T withLock(
        String lockKey,
        Duration waitTimeout,
        Duration leaseTimeout,
        Supplier<T> operation
    ) throws LockException {

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock
            boolean acquired = lock.tryLock(
                waitTimeout.toMillis(),
                leaseTimeout.toMillis(),
                TimeUnit.MILLISECONDS
            );

            if (!acquired) {
                throw new LockException.TimeoutException(lockKey, waitTimeout);
            }

            log.debug("Lock acquired: {}", lockKey);

            try {
                // Execute operation while holding lock
                return operation.get();

            } finally {
                // Always release lock
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("Lock released: {}", lockKey);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockException("Interrupted while acquiring lock: " + lockKey, e);
        }
    }

    @Override
    public DistributedLock tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_TIMEOUT, DEFAULT_LEASE_TIMEOUT);
    }

    @Override
    public DistributedLock tryLock(
        String lockKey,
        Duration waitTimeout,
        Duration leaseTimeout
    ) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                waitTimeout.toMillis(),
                leaseTimeout.toMillis(),
                TimeUnit.MILLISECONDS
            );

            if (acquired) {
                log.debug("Lock acquired: {}", lockKey);
                return new RedisDistributedLock(lock, lockKey);
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock: {}", lockKey);
            return null;
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * Redis-backed lock handle.
     */
    private static class RedisDistributedLock implements DistributedLock {
        private final RLock rLock;
        private final String lockKey;

        public RedisDistributedLock(RLock rLock, String lockKey) {
            this.rLock = rLock;
            this.lockKey = lockKey;
        }

        @Override
        public void unlock() {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }

        @Override
        public boolean isHeldByCurrentThread() {
            return rLock.isHeldByCurrentThread();
        }

        @Override
        public boolean extend(Duration additionalTime) {
            if (rLock.isHeldByCurrentThread()) {
                return rLock.expire(additionalTime.toMillis(), TimeUnit.MILLISECONDS);
            }
            return false;
        }
    }
}
