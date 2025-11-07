package io.tresor.core.secretsharing;

import io.tresor.core.encryption.EncryptionException;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Quick standalone test for Shamir's Secret Sharing.
 * Can be run without Maven/JUnit.
 */
public class QuickShamirTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Shamir's Secret Sharing Test ===\n");

            ShamirSecretSharing shamir = new ShamirSecretSharing();
            SecureRandom random = new SecureRandom();

            // Test 1: Basic split and reconstruct
            System.out.println("Test 1: Basic split and reconstruct");
            byte[] secret = new byte[32]; // AES-256 key
            random.nextBytes(secret);
            System.out.println("Original secret (first 8 bytes): " + bytesToHex(Arrays.copyOf(secret, 8)));

            List<ShamirSecretSharing.Share> shares = shamir.split(secret, 5, 3);
            System.out.println("Created " + shares.size() + " shares with threshold 3");

            byte[] reconstructed = shamir.reconstruct(shares);
            System.out.println("Reconstructed (first 8 bytes): " + bytesToHex(Arrays.copyOf(reconstructed, 8)));

            boolean test1Pass = Arrays.equals(secret, reconstructed);
            System.out.println("✓ Test 1: " + (test1Pass ? "PASS" : "FAIL") + "\n");

            // Test 2: Reconstruct with minimum shares
            System.out.println("Test 2: Reconstruct with minimum shares (3 of 5)");
            List<ShamirSecretSharing.Share> minimalShares = shares.subList(0, 3);
            byte[] reconstructedMin = shamir.reconstruct(minimalShares);

            boolean test2Pass = Arrays.equals(secret, reconstructedMin);
            System.out.println("✓ Test 2: " + (test2Pass ? "PASS" : "FAIL") + "\n");

            // Test 3: Different share combinations
            System.out.println("Test 3: Different share combinations");
            boolean test3Pass = true;

            // Test combination: shares 1, 3, 5
            List<ShamirSecretSharing.Share> combo1 = Arrays.asList(
                shares.get(0), shares.get(2), shares.get(4)
            );
            byte[] recon1 = shamir.reconstruct(combo1);
            System.out.println("  Shares 1,3,5: " + Arrays.equals(secret, recon1));
            test3Pass &= Arrays.equals(secret, recon1);

            // Test combination: shares 2, 3, 4
            List<ShamirSecretSharing.Share> combo2 = Arrays.asList(
                shares.get(1), shares.get(2), shares.get(3)
            );
            byte[] recon2 = shamir.reconstruct(combo2);
            System.out.println("  Shares 2,3,4: " + Arrays.equals(secret, recon2));
            test3Pass &= Arrays.equals(secret, recon2);

            System.out.println("✓ Test 3: " + (test3Pass ? "PASS" : "FAIL") + "\n");

            // Test 4: Insufficient shares should fail
            System.out.println("Test 4: Insufficient shares (should fail)");
            boolean test4Pass = false;
            try {
                List<ShamirSecretSharing.Share> insufficient = shares.subList(0, 2);
                shamir.reconstruct(insufficient);
                System.out.println("  ERROR: Should have thrown exception!");
            } catch (EncryptionException e) {
                System.out.println("  Correctly rejected: " + e.getMessage());
                test4Pass = true;
            }
            System.out.println("✓ Test 4: " + (test4Pass ? "PASS" : "FAIL") + "\n");

            // Test 5: Share serialization
            System.out.println("Test 5: Share serialization");
            ShamirSecretSharing.Share originalShare = shares.get(0);
            byte[] serialized = originalShare.toBytes();
            System.out.println("  Serialized size: " + serialized.length + " bytes");

            ShamirSecretSharing.Share deserialized = ShamirSecretSharing.Share.fromBytes(
                serialized, originalShare.getThreshold(), originalShare.getTotalShares()
            );

            boolean test5Pass = originalShare.getX() == deserialized.getX() &&
                                originalShare.getY().equals(deserialized.getY());
            System.out.println("✓ Test 5: " + (test5Pass ? "PASS" : "FAIL") + "\n");

            // Summary
            System.out.println("=== SUMMARY ===");
            int passed = 0;
            if (test1Pass) passed++;
            if (test2Pass) passed++;
            if (test3Pass) passed++;
            if (test4Pass) passed++;
            if (test5Pass) passed++;

            System.out.println("Passed: " + passed + "/5 tests");

            if (passed == 5) {
                System.out.println("\n✓ All tests PASSED! Shamir's Secret Sharing is working correctly.");
                System.exit(0);
            } else {
                System.out.println("\n✗ Some tests FAILED!");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
