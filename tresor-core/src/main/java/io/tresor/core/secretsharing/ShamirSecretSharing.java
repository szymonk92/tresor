package io.tresor.core.secretsharing;

import io.tresor.core.encryption.EncryptionException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Shamir's Secret Sharing (1979).
 *
 * Allows splitting a secret into n shares where any k shares
 * can reconstruct the original secret, but k-1 shares reveal nothing.
 *
 * How it works:
 * 1. Represent secret as number S
 * 2. Generate random polynomial P(x) of degree k-1 where P(0) = S
 *    P(x) = a₀ + a₁x + a₂x² + ... + aₖ₋₁x^(k-1)
 *    where a₀ = S, and a₁...aₖ₋₁ are random
 * 3. Create n shares by evaluating P(1), P(2), ..., P(n)
 * 4. To reconstruct: Use Lagrange interpolation on any k shares to find P(0) = S
 *
 * Security:
 * - Information-theoretically secure (not just computationally)
 * - k-1 shares reveal ZERO information about secret
 * - Works in finite field (modulo prime p)
 */
@Slf4j
public class ShamirSecretSharing {

    // Large prime for finite field arithmetic (256 bits)
    // All arithmetic is done modulo this prime
    private static final BigInteger PRIME = new BigInteger(
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16
    ); // secp256k1 field prime

    private final SecureRandom secureRandom;

    public ShamirSecretSharing() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Split a secret into n shares with threshold k.
     *
     * @param secret The secret to split (e.g., AES-256 key)
     * @param n Total number of shares to create
     * @param k Minimum number of shares needed to reconstruct (threshold)
     * @return List of n shares
     * @throws EncryptionException if parameters are invalid
     */
    public List<Share> split(byte[] secret, int n, int k) throws EncryptionException {
        if (k > n) {
            throw new EncryptionException("Threshold k cannot be greater than total shares n");
        }
        if (k < 2) {
            throw new EncryptionException("Threshold must be at least 2");
        }
        if (n < 2) {
            throw new EncryptionException("Must create at least 2 shares");
        }

        log.info("Splitting secret into {} shares with threshold {}", n, k);

        // Convert secret bytes to BigInteger
        BigInteger secretInt = new BigInteger(1, secret);

        // Ensure secret is less than prime
        if (secretInt.compareTo(PRIME) >= 0) {
            throw new EncryptionException("Secret is too large for field");
        }

        // Generate random polynomial coefficients
        // P(x) = a₀ + a₁x + a₂x² + ... + aₖ₋₁x^(k-1)
        // where a₀ = secret
        BigInteger[] coefficients = new BigInteger[k];
        coefficients[0] = secretInt; // Secret is y-intercept

        for (int i = 1; i < k; i++) {
            // Generate random coefficients in field [1, PRIME-1]
            coefficients[i] = new BigInteger(PRIME.bitLength(), secureRandom).mod(PRIME);
        }

        log.debug("Generated polynomial of degree {}", k - 1);

        // Create n shares by evaluating polynomial at x = 1, 2, ..., n
        List<Share> shares = new ArrayList<>();
        for (int x = 1; x <= n; x++) {
            BigInteger xBig = BigInteger.valueOf(x);
            BigInteger y = evaluatePolynomial(coefficients, xBig, PRIME);

            shares.add(Share.builder()
                .x(x)
                .y(y)
                .threshold(k)
                .totalShares(n)
                .build());
        }

        log.info("Created {} shares successfully", n);
        return shares;
    }

    /**
     * Reconstruct secret from k or more shares using Lagrange interpolation.
     *
     * @param shares At least k shares (can be more)
     * @return The reconstructed secret
     * @throws EncryptionException if insufficient shares or invalid data
     */
    public byte[] reconstruct(List<Share> shares) throws EncryptionException {
        if (shares == null || shares.isEmpty()) {
            throw new EncryptionException("No shares provided");
        }

        int threshold = shares.get(0).getThreshold();

        if (shares.size() < threshold) {
            throw new EncryptionException(
                "Insufficient shares: have " + shares.size() + ", need " + threshold
            );
        }

        log.info("Reconstructing secret from {} shares (threshold: {})", shares.size(), threshold);

        // Use only threshold number of shares (first k)
        List<Share> selectedShares = shares.subList(0, threshold);

        // Use Lagrange interpolation to find P(0)
        // P(0) = Σᵢ yᵢ · Πⱼ≠ᵢ (0 - xⱼ) / (xᵢ - xⱼ)
        BigInteger secret = BigInteger.ZERO;

        for (int i = 0; i < selectedShares.size(); i++) {
            Share shareI = selectedShares.get(i);
            BigInteger xi = BigInteger.valueOf(shareI.getX());
            BigInteger yi = shareI.getY();

            // Calculate Lagrange basis polynomial at x = 0
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < selectedShares.size(); j++) {
                if (i != j) {
                    Share shareJ = selectedShares.get(j);
                    BigInteger xj = BigInteger.valueOf(shareJ.getX());

                    // Numerator: (0 - xⱼ) = -xⱼ
                    numerator = numerator.multiply(xj.negate()).mod(PRIME);

                    // Denominator: (xᵢ - xⱼ)
                    denominator = denominator.multiply(xi.subtract(xj)).mod(PRIME);
                }
            }

            // Compute basis polynomial: numerator / denominator
            // In finite field, division is multiplication by modular inverse
            BigInteger basis = numerator.multiply(denominator.modInverse(PRIME)).mod(PRIME);

            // Add term: yᵢ · basis
            secret = secret.add(yi.multiply(basis)).mod(PRIME);
        }

        log.info("Secret reconstructed successfully");

        // Convert back to bytes
        byte[] secretBytes = secret.toByteArray();

        // Remove sign byte if present
        if (secretBytes.length > 0 && secretBytes[0] == 0) {
            byte[] trimmed = new byte[secretBytes.length - 1];
            System.arraycopy(secretBytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }

        return secretBytes;
    }

    /**
     * Evaluate polynomial at given x value.
     * P(x) = a₀ + a₁x + a₂x² + ... + aₙx^n (mod p)
     */
    private BigInteger evaluatePolynomial(BigInteger[] coefficients, BigInteger x, BigInteger prime) {
        BigInteger result = BigInteger.ZERO;
        BigInteger xPower = BigInteger.ONE; // x^0 = 1

        for (BigInteger coefficient : coefficients) {
            // result += coefficient * x^i
            result = result.add(coefficient.multiply(xPower)).mod(prime);

            // xPower = x^(i+1)
            xPower = xPower.multiply(x).mod(prime);
        }

        return result;
    }

    /**
     * A share of the secret.
     */
    @Data
    @Builder
    public static class Share {
        /**
         * X coordinate (share number: 1, 2, 3, ...)
         */
        private int x;

        /**
         * Y coordinate (P(x))
         */
        private BigInteger y;

        /**
         * Prime modulus used for this share
         */
        private BigInteger prime;

        /**
         * Threshold (minimum shares needed to reconstruct)
         */
        private int threshold;

        /**
         * Total number of shares created
         */
        private int totalShares;

        /**
         * Constructor for creating a share
         */
        public Share(int x, BigInteger y, int threshold, int totalShares) {
            this.x = x;
            this.y = y;
            this.threshold = threshold;
            this.totalShares = totalShares;
        }

        /**
         * Full constructor with all fields
         */
        public Share(int x, BigInteger y, BigInteger prime, int threshold, int totalShares) {
            this.x = x;
            this.y = y;
            this.prime = prime;
            this.threshold = threshold;
            this.totalShares = totalShares;
        }

        /**
         * Convert share to bytes for storage/transmission.
         */
        public byte[] toBytes() {
            byte[] yBytes = y.toByteArray();
            byte[] result = new byte[yBytes.length + 4];

            // Store x as 4 bytes
            result[0] = (byte) (x >> 24);
            result[1] = (byte) (x >> 16);
            result[2] = (byte) (x >> 8);
            result[3] = (byte) x;

            // Store y bytes
            System.arraycopy(yBytes, 0, result, 4, yBytes.length);

            return result;
        }

        /**
         * Reconstruct share from bytes.
         */
        public static Share fromBytes(byte[] data, int threshold, int totalShares) {
            // Read x (4 bytes)
            int x = ((data[0] & 0xFF) << 24) |
                    ((data[1] & 0xFF) << 16) |
                    ((data[2] & 0xFF) << 8) |
                    (data[3] & 0xFF);

            // Read y (remaining bytes)
            byte[] yBytes = new byte[data.length - 4];
            System.arraycopy(data, 4, yBytes, 0, yBytes.length);
            BigInteger y = new BigInteger(yBytes);

            return Share.builder()
                .x(x)
                .y(y)
                .threshold(threshold)
                .totalShares(totalShares)
                .build();
        }
    }
}
