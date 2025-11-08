package io.tresor.core.encryption.impl;

import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.model.EncryptedMessage;
import io.tresor.core.model.PlainMessage;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Password-based encryption service.
 *
 * Uses PBKDF2 to derive encryption key from user's password.
 * Simpler than Shamir Secret Sharing, perfect for 90% of users.
 *
 * Security:
 * - PBKDF2-HMAC-SHA256 with 100,000 iterations
 * - Random 32-byte salt per message
 * - AES-256-GCM authenticated encryption
 * - Password never stored (only salt + hint)
 *
 * Trade-offs:
 * - ✅ Much simpler UX (user just remembers password)
 * - ✅ Cheaper ($0.50 vs $0.57 - no blockchain key management)
 * - ⚠️ Security depends on password strength
 * - ❌ If password forgotten, message lost forever
 */
@Slf4j
public class PasswordEncryptionService {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 100_000; // OWASP recommendation
    private static final int SALT_LENGTH = 32; // 256 bits
    private static final int KEY_LENGTH = 256; // 256 bits for AES-256
    private static final int IV_LENGTH = 12; // 96 bits for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final SecureRandom secureRandom;

    public PasswordEncryptionService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt message with password.
     *
     * @param message Plain message
     * @param password User's password
     * @param passwordHint Optional hint (NOT the password itself!)
     * @return Encrypted message with salt
     */
    public PasswordEncryptedMessage encrypt(
        PlainMessage message,
        String password,
        String passwordHint
    ) throws EncryptionException {

        try {
            // Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            // Derive encryption key from password
            SecretKey key = deriveKey(password, salt);

            // Generate random IV
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Serialize message
            byte[] plaintext = serializeMessage(message);

            // Encrypt with AES-256-GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Calculate content hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] contentHash = digest.digest(plaintext);

            log.info("Message encrypted with password (salt: {}, iv: {})",
                bytesToHex(salt).substring(0, 16) + "...",
                bytesToHex(iv).substring(0, 16) + "...");

            return PasswordEncryptedMessage.builder()
                .id(UUID.randomUUID().toString())
                .encryptedPayload(ciphertext)
                .iv(iv)
                .salt(salt)
                .iterations(PBKDF2_ITERATIONS)
                .passwordHint(passwordHint)
                .contentHash(contentHash)
                .build();

        } catch (Exception e) {
            throw new EncryptionException("Password encryption failed", e);
        }
    }

    /**
     * Decrypt message with password.
     *
     * @param encrypted Encrypted message
     * @param password User's password
     * @return Plain message
     */
    public PlainMessage decrypt(
        PasswordEncryptedMessage encrypted,
        String password
    ) throws EncryptionException {

        try {
            // Derive key from password + salt
            SecretKey key = deriveKey(password, encrypted.getSalt());

            // Decrypt with AES-256-GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, encrypted.getIv());
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(encrypted.getEncryptedPayload());

            // Verify content hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] computedHash = digest.digest(plaintext);

            if (!java.util.Arrays.equals(computedHash, encrypted.getContentHash())) {
                throw new EncryptionException("Content hash mismatch - message corrupted");
            }

            // Deserialize message
            PlainMessage message = deserializeMessage(plaintext);

            log.info("Message decrypted successfully");

            return message;

        } catch (javax.crypto.AEADBadTagException e) {
            throw new EncryptionException("Incorrect password or corrupted ciphertext", e);
        } catch (Exception e) {
            throw new EncryptionException("Password decryption failed", e);
        }
    }

    /**
     * Derive encryption key from password using PBKDF2.
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        spec.clearPassword(); // Security: clear password from memory

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Serialize message to bytes.
     */
    private byte[] serializeMessage(PlainMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write text content
        byte[] textBytes = message.getTextContent().getBytes("UTF-8");
        writeInt(baos, textBytes.length);
        baos.write(textBytes);

        // Write delivery email
        byte[] emailBytes = message.getDeliveryEmail().getBytes("UTF-8");
        writeInt(baos, emailBytes.length);
        baos.write(emailBytes);

        // Write file count
        int fileCount = message.getFiles() != null ? message.getFiles().size() : 0;
        writeInt(baos, fileCount);

        // Write files (if any)
        if (message.getFiles() != null) {
            for (PlainMessage.AttachedFile file : message.getFiles()) {
                // File name
                byte[] nameBytes = file.getFileName().getBytes("UTF-8");
                writeInt(baos, nameBytes.length);
                baos.write(nameBytes);

                // File content
                writeInt(baos, file.getFileData().length);
                baos.write(file.getFileData());

                // Content type
                byte[] typeBytes = file.getContentType().getBytes("UTF-8");
                writeInt(baos, typeBytes.length);
                baos.write(typeBytes);
            }
        }

        return baos.toByteArray();
    }

    /**
     * Deserialize message from bytes.
     */
    private PlainMessage deserializeMessage(byte[] data) throws IOException {
        int offset = 0;

        // Read text content
        int textLength = readInt(data, offset);
        offset += 4;
        String textContent = new String(data, offset, textLength, "UTF-8");
        offset += textLength;

        // Read delivery email
        int emailLength = readInt(data, offset);
        offset += 4;
        String deliveryEmail = new String(data, offset, emailLength, "UTF-8");
        offset += emailLength;

        // Read file count
        int fileCount = readInt(data, offset);
        offset += 4;

        // Read files
        java.util.List<PlainMessage.AttachedFile> files = new java.util.ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            // File name
            int nameLength = readInt(data, offset);
            offset += 4;
            String fileName = new String(data, offset, nameLength, "UTF-8");
            offset += nameLength;

            // File content
            int dataLength = readInt(data, offset);
            offset += 4;
            byte[] fileData = new byte[dataLength];
            System.arraycopy(data, offset, fileData, 0, dataLength);
            offset += dataLength;

            // Content type
            int typeLength = readInt(data, offset);
            offset += 4;
            String contentType = new String(data, offset, typeLength, "UTF-8");
            offset += typeLength;

            files.add(new PlainMessage.AttachedFile(fileName, fileData, contentType));
        }

        return PlainMessage.builder()
            .textContent(textContent)
            .deliveryEmail(deliveryEmail)
            .files(files)
            .createdAt(java.time.LocalDateTime.now())
            .build();
    }

    private void writeInt(ByteArrayOutputStream baos, int value) {
        baos.write((value >>> 24) & 0xFF);
        baos.write((value >>> 16) & 0xFF);
        baos.write((value >>> 8) & 0xFF);
        baos.write(value & 0xFF);
    }

    private int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Password-encrypted message with salt and metadata.
     */
    @lombok.Data
    @lombok.Builder
    public static class PasswordEncryptedMessage {
        private String id;
        private byte[] encryptedPayload;
        private byte[] iv;
        private byte[] salt;
        private int iterations;
        private String passwordHint;
        private byte[] contentHash;
    }
}
