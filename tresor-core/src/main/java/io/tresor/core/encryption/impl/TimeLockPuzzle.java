package io.tresor.core.encryption.impl;

import io.tresor.core.encryption.EncryptionException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Implementation of Rivest-Shamir-Wagner Time-Lock Puzzle (1996).
 *
 * The puzzle requires sequential computation to solve, making it impossible to
 * decrypt early even with massive parallel computing resources.
 *
 * How it works:
 * 1. Generate large RSA modulus N = p * q
 * 2. Choose random message key K
 * 3. Compute puzzle: C = K + 2^t mod N
 *    where t = time_delay_seconds × squarings_per_second
 * 4. To unlock: Compute 2^t mod N (requires t sequential squarings)
 * 5. Recover key: K = C - 2^t mod N
 *
 * Security:
 * - Breaking RSA is hard (factoring N)
 * - Sequential squaring cannot be parallelized
 * - Only the creator knows p and q (can compute φ(N) for fast solving)
 */
@Slf4j
public class TimeLockPuzzle {

    // RSA modulus size in bits (2048 is current standard)
    private static final int RSA_KEY_SIZE = 2048;

    // Estimated squarings per second on modern hardware (2025)
    // Conservative estimate: ~1 billion squarings/second on single core
    private static final long SQUARINGS_PER_SECOND = 1_000_000_000L;

    private final SecureRandom secureRandom;

    public TimeLockPuzzle() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Create a time-lock puzzle that encrypts a key.
     *
     * @param key The secret key to encrypt (32 bytes for AES-256)
     * @param delaySeconds How long the puzzle should take to solve
     * @return The puzzle parameters needed for solving
     */
    public PuzzleParams create(byte[] key, long delaySeconds) throws EncryptionException {
        try {
            log.info("Creating time-lock puzzle for {} second delay (~{} years)",
                delaySeconds, delaySeconds / (365.25 * 24 * 3600));

            // Generate two large primes p and q
            log.debug("Generating RSA primes...");
            BigInteger p = BigInteger.probablePrime(RSA_KEY_SIZE / 2, secureRandom);
            BigInteger q = BigInteger.probablePrime(RSA_KEY_SIZE / 2, secureRandom);

            // Compute RSA modulus N = p × q
            BigInteger n = p.multiply(q);

            // Compute φ(N) = (p-1)(q-1) - Euler's totient function
            // This allows creator to solve puzzle quickly
            BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

            // Calculate difficulty: number of sequential squarings required
            BigInteger t = BigInteger.valueOf(delaySeconds).multiply(BigInteger.valueOf(SQUARINGS_PER_SECOND));

            log.debug("Puzzle difficulty: 2^{} squarings", t);

            // Convert key bytes to BigInteger
            BigInteger keyInt = new BigInteger(1, key);

            // Ensure key is smaller than N
            if (keyInt.compareTo(n) >= 0) {
                throw new EncryptionException("Key is too large for RSA modulus");
            }

            // Compute puzzle: C = K + 2^t mod N
            // Creator can compute this quickly using: 2^(t mod φ(N)) mod N
            BigInteger a = BigInteger.valueOf(2).modPow(t.mod(phi), n);
            BigInteger cipherKey = keyInt.add(a).mod(n);

            log.info("Time-lock puzzle created successfully");

            return PuzzleParams.builder()
                .n(n)
                .cipherKey(cipherKey)
                .difficulty(t)
                .createdAt(System.currentTimeMillis())
                .squaringsPerSecond(SQUARINGS_PER_SECOND)
                .build();

        } catch (Exception e) {
            throw new EncryptionException("Failed to create time-lock puzzle", e);
        }
    }

    /**
     * Solve a time-lock puzzle to recover the encrypted key.
     *
     * WARNING: This requires sequential computation and will take
     * approximately the delay time specified when creating the puzzle.
     *
     * @param params The puzzle parameters
     * @param keyLength Expected key length in bytes (32 for AES-256)
     * @return The decrypted key
     */
    public byte[] solve(PuzzleParams params, int keyLength) throws EncryptionException {
        try {
            log.info("Solving time-lock puzzle with difficulty 2^{}", params.getDifficulty());
            log.warn("This will take approximately {} seconds (~{} days)",
                params.getDifficulty().divide(BigInteger.valueOf(SQUARINGS_PER_SECOND)),
                params.getDifficulty().divide(BigInteger.valueOf(SQUARINGS_PER_SECOND * 86400)));

            // Compute 2^t mod N through sequential squaring
            // This CANNOT be parallelized - each squaring depends on previous result
            BigInteger result = BigInteger.valueOf(2);
            BigInteger remaining = params.getDifficulty();

            long startTime = System.currentTimeMillis();
            long logInterval = params.getDifficulty().divide(BigInteger.valueOf(100)).longValue(); // Log every 1%
            long nextLog = logInterval;

            // Perform t sequential squarings: result = 2^(2^t) mod N
            for (BigInteger i = BigInteger.ZERO; i.compareTo(remaining) < 0; i = i.add(BigInteger.ONE)) {
                result = result.modPow(BigInteger.valueOf(2), params.getN());

                // Log progress periodically
                if (i.longValue() >= nextLog) {
                    double progress = i.doubleValue() / remaining.doubleValue() * 100;
                    long elapsed = System.currentTimeMillis() - startTime;
                    long estimated = (long) (elapsed / progress * 100);
                    log.info("Progress: {:.2f}% - Elapsed: {}s - Estimated total: {}s",
                        progress, elapsed / 1000, estimated / 1000);
                    nextLog += logInterval;
                }
            }

            // Recover key: K = C - 2^t mod N
            BigInteger keyInt = params.getCipherKey().subtract(result).mod(params.getN());

            log.info("Puzzle solved in {} seconds", (System.currentTimeMillis() - startTime) / 1000);

            // Convert to byte array
            byte[] keyBytes = keyInt.toByteArray();

            // BigInteger may add a sign byte - remove if present
            if (keyBytes.length > keyLength && keyBytes[0] == 0) {
                byte[] trimmed = new byte[keyLength];
                System.arraycopy(keyBytes, 1, trimmed, 0, keyLength);
                return trimmed;
            }

            // Pad with zeros if needed
            if (keyBytes.length < keyLength) {
                byte[] padded = new byte[keyLength];
                System.arraycopy(keyBytes, 0, padded, keyLength - keyBytes.length, keyBytes.length);
                return padded;
            }

            return keyBytes;

        } catch (Exception e) {
            throw new EncryptionException("Failed to solve time-lock puzzle", e);
        }
    }

    /**
     * Calculate the target difficulty for a given time delay.
     */
    public static BigInteger calculateDifficulty(long delaySeconds) {
        return BigInteger.valueOf(delaySeconds).multiply(BigInteger.valueOf(SQUARINGS_PER_SECOND));
    }

    /**
     * Estimate time to solve based on difficulty and current hardware.
     */
    public static long estimateSolveTime(BigInteger difficulty) {
        return difficulty.divide(BigInteger.valueOf(SQUARINGS_PER_SECOND)).longValue();
    }

    /**
     * Parameters needed to solve a time-lock puzzle.
     */
    @Data
    @Builder
    public static class PuzzleParams {
        /**
         * RSA modulus N
         */
        private BigInteger n;

        /**
         * Encrypted key: C = K + 2^t mod N
         */
        private BigInteger cipherKey;

        /**
         * Difficulty: number of sequential squarings required
         */
        private BigInteger difficulty;

        /**
         * When the puzzle was created (Unix timestamp)
         */
        private long createdAt;

        /**
         * Squarings per second used to calibrate difficulty
         */
        private long squaringsPerSecond;
    }
}
