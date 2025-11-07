package io.tresor.core.secretsharing;

import io.tresor.core.encryption.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Shamir's Secret Sharing implementation.
 */
class ShamirSecretSharingTest {

    private ShamirSecretSharing shamir;
    private SecureRandom random;

    @BeforeEach
    void setUp() {
        shamir = new ShamirSecretSharing();
        random = new SecureRandom();
    }

    @Test
    void testSplitAndReconstruct_SimpleCase() throws Exception {
        // Generate AES-256 key
        byte[] secret = generateRandomKey(32);

        // Split into 5 shares with threshold 3
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);

        assertEquals(5, shares.size(), "Should create 5 shares");

        // Reconstruct from all shares
        byte[] reconstructed = shamir.reconstruct(shares);

        assertArrayEquals(secret, reconstructed, "Reconstructed secret should match original");
    }

    @Test
    void testReconstruct_WithMinimumShares() throws Exception {
        byte[] secret = generateRandomKey(32);

        // Split into 5 shares with threshold 3
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);

        // Use only threshold shares (first 3)
        List<ShamirSecretSharing.Share> minimalShares = shares.subList(0, 3);
        byte[] reconstructed = shamir.reconstruct(minimalShares);

        assertArrayEquals(secret, reconstructed, "Should reconstruct with minimum shares");
    }

    @Test
    void testReconstruct_WithDifferentShareCombinations() throws Exception {
        byte[] secret = generateRandomKey(32);

        // Split into 5 shares with threshold 3
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);

        // Test all combinations of 3 shares
        int[][] combinations = {
            {0, 1, 2}, {0, 1, 3}, {0, 1, 4},
            {0, 2, 3}, {0, 2, 4}, {0, 3, 4},
            {1, 2, 3}, {1, 2, 4}, {1, 3, 4},
            {2, 3, 4}
        };

        for (int[] combo : combinations) {
            List<ShamirSecretSharing.Share> selectedShares = new ArrayList<>();
            for (int index : combo) {
                selectedShares.add(shares.get(index));
            }

            byte[] reconstructed = shamir.reconstruct(selectedShares);
            assertArrayEquals(secret, reconstructed,
                "Combination " + java.util.Arrays.toString(combo) + " should reconstruct correctly");
        }
    }

    @Test
    void testReconstruct_InsufficientShares() throws Exception {
        byte[] secret = generateRandomKey(32);

        // Split into 5 shares with threshold 3
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);

        // Try to reconstruct with only 2 shares (below threshold)
        List<ShamirSecretSharing.Share> insufficientShares = shares.subList(0, 2);

        EncryptionException exception = assertThrows(
            EncryptionException.class,
            () -> shamir.reconstruct(insufficientShares),
            "Should throw exception with insufficient shares"
        );

        assertTrue(exception.getMessage().contains("Insufficient shares"));
    }

    @Test
    void testSplit_InvalidParameters() {
        byte[] secret = generateRandomKey(32);

        // Threshold greater than total shares
        assertThrows(EncryptionException.class,
            () -> shamir.split(secret, 3, 5),
            "Should reject k > n");

        // Threshold too small
        assertThrows(EncryptionException.class,
            () -> shamir.split(secret, 5, 1),
            "Should reject k < 2");

        // Total shares too small
        assertThrows(EncryptionException.class,
            () -> shamir.split(secret, 1, 1),
            "Should reject n < 2");
    }

    @Test
    void testShareSerialization() throws Exception {
        byte[] secret = generateRandomKey(32);

        // Split secret
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);

        // Serialize and deserialize each share
        List<ShamirSecretSharing.Share> deserializedShares = new ArrayList<>();
        for (ShamirSecretSharing.Share share : shares) {
            byte[] serialized = share.toBytes();
            ShamirSecretSharing.Share deserialized = ShamirSecretSharing.Share.fromBytes(
                serialized, share.getThreshold(), share.getTotalShares()
            );
            deserializedShares.add(deserialized);

            // Verify share data matches
            assertEquals(share.getX(), deserialized.getX(), "X coordinate should match");
            assertEquals(share.getY(), deserialized.getY(), "Y coordinate should match");
        }

        // Verify reconstruction works with deserialized shares
        byte[] reconstructed = shamir.reconstruct(deserializedShares);
        assertArrayEquals(secret, reconstructed, "Should reconstruct from serialized shares");
    }

    @Test
    void testDifferentSecretSizes() throws Exception {
        // Test with different key sizes
        int[] keySizes = {16, 24, 32}; // 128-bit, 192-bit, 256-bit

        for (int size : keySizes) {
            byte[] secret = generateRandomKey(size);

            List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);
            byte[] reconstructed = shamir.reconstruct(shares);

            assertArrayEquals(secret, reconstructed,
                "Should work with " + (size * 8) + "-bit key");
        }
    }

    @Test
    void testLargeThreshold() throws Exception {
        byte[] secret = generateRandomKey(32);

        // Split into 10 shares with high threshold
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 10, 8);

        assertEquals(10, shares.size());

        // Reconstruct with exactly threshold shares
        byte[] reconstructed = shamir.reconstruct(shares.subList(0, 8));
        assertArrayEquals(secret, reconstructed);

        // Verify insufficient shares fails
        assertThrows(EncryptionException.class,
            () -> shamir.reconstruct(shares.subList(0, 7)),
            "Should fail with 7 shares when threshold is 8");
    }

    @Test
    void testSecurityProperty_KMinusOneSharesRevealNothing() throws Exception {
        byte[] secret = generateRandomKey(32);

        // Split into 5 shares with threshold 3
        List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);

        // Having 2 shares (k-1) should not allow reconstruction
        List<ShamirSecretSharing.Share> twoShares = shares.subList(0, 2);

        assertThrows(EncryptionException.class,
            () -> shamir.reconstruct(twoShares),
            "k-1 shares should not be sufficient");
    }

    @Test
    void testMultipleSecrets_Independence() throws Exception {
        // Split two different secrets
        byte[] secret1 = generateRandomKey(32);
        byte[] secret2 = generateRandomKey(32);

        List<ShamirSecretSharing.Share> shares1 = shamir.split(secret1, 5, 3);
        List<ShamirSecretSharing.Share> shares2 = shamir.split(secret2, 5, 3);

        // Reconstruct each independently
        byte[] reconstructed1 = shamir.reconstruct(shares1.subList(0, 3));
        byte[] reconstructed2 = shamir.reconstruct(shares2.subList(0, 3));

        assertArrayEquals(secret1, reconstructed1, "Should reconstruct secret 1 correctly");
        assertArrayEquals(secret2, reconstructed2, "Should reconstruct secret 2 correctly");
        assertFalse(java.util.Arrays.equals(secret1, secret2), "Secrets should be different");
    }

    @Test
    void testRealWorldScenario_AesKey() throws Exception {
        // Generate actual AES-256 key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();
        byte[] keyBytes = aesKey.getEncoded();

        System.out.println("Original AES key length: " + keyBytes.length + " bytes");

        // Split into 5 shares (Bitcoin, Ethereum, Arbitrum, Self-custody, Arweave)
        // Threshold 3 (any 3 can reconstruct)
        List<ShamirSecretSharing.Share> shares = shamir.split(keyBytes, 5, 3);

        System.out.println("Created " + shares.size() + " shares with threshold 3");
        for (int i = 0; i < shares.size(); i++) {
            ShamirSecretSharing.Share share = shares.get(i);
            System.out.println("Share " + (i + 1) + ": x=" + share.getX() +
                ", y_size=" + share.getY().bitLength() + " bits");
        }

        // Simulate: Bitcoin, Ethereum, and Arbitrum are available
        List<ShamirSecretSharing.Share> availableShares = new ArrayList<>();
        availableShares.add(shares.get(0)); // Bitcoin
        availableShares.add(shares.get(1)); // Ethereum
        availableShares.add(shares.get(2)); // Arbitrum

        // Reconstruct
        byte[] reconstructedKey = shamir.reconstruct(availableShares);

        System.out.println("Reconstructed key length: " + reconstructedKey.length + " bytes");

        assertArrayEquals(keyBytes, reconstructedKey, "Should reconstruct AES key perfectly");
    }

    /**
     * Helper: Generate random key of specified size.
     */
    private byte[] generateRandomKey(int sizeBytes) {
        byte[] key = new byte[sizeBytes];
        random.nextBytes(key);
        return key;
    }
}
