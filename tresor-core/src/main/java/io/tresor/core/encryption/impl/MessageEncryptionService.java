package io.tresor.core.encryption.impl;

import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.model.EncryptedMessage;
import io.tresor.core.model.PlainMessage;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Complete message encryption service.
 *
 * Handles encryption of messages including text and attachments.
 * Uses AES-256-GCM for authenticated encryption.
 */
@Slf4j
public class MessageEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // bits
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes

    private final SecureRandom secureRandom;

    public MessageEncryptionService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt a plain message.
     *
     * @param message The message to encrypt
     * @return Encrypted message with metadata
     * @throws EncryptionException if encryption fails
     */
    public EncryptedMessage encrypt(PlainMessage message) throws EncryptionException {
        try {
            log.info("Encrypting message with {} attachments", message.getFiles().size());

            // Generate encryption key
            SecretKey key = generateKey();

            // Generate IV
            byte[] iv = generateIV();

            // Serialize message to bytes
            byte[] plaintext = serializeMessage(message);

            // Encrypt
            byte[] ciphertext = encrypt(plaintext, key, iv);

            // Calculate hash
            byte[] contentHash = calculateHash(plaintext);

            log.info("Message encrypted: {} bytes → {} bytes",
                plaintext.length, ciphertext.length);

            return EncryptedMessage.builder()
                .id(UUID.randomUUID().toString())
                .version("1.0")
                .encryptedPayload(ciphertext)
                .encryptionKey(key.getEncoded())
                .iv(iv)
                .contentHash(contentHash)
                .createdAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt message", e);
        }
    }

    /**
     * Decrypt an encrypted message.
     *
     * @param encrypted The encrypted message
     * @param key The decryption key
     * @return Decrypted plain message
     * @throws EncryptionException if decryption fails
     */
    public PlainMessage decrypt(EncryptedMessage encrypted, byte[] key) throws EncryptionException {
        try {
            log.info("Decrypting message: {}", encrypted.getId());

            // Recreate SecretKey
            SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);

            // Decrypt
            byte[] plaintext = decrypt(encrypted.getEncryptedPayload(), secretKey, encrypted.getIv());

            // Verify hash
            byte[] actualHash = calculateHash(plaintext);
            if (!java.util.Arrays.equals(actualHash, encrypted.getContentHash())) {
                throw new EncryptionException("Content hash mismatch - possible tampering");
            }

            // Deserialize
            PlainMessage message = deserializeMessage(plaintext);

            log.info("Message decrypted: {} bytes", plaintext.length);

            return message;

        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt message", e);
        }
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, secureRandom);
        return keyGenerator.generateKey();
    }

    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        return cipher.doFinal(plaintext);
    }

    private byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        return cipher.doFinal(ciphertext);
    }

    private byte[] calculateHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    /**
     * Serialize message to bytes.
     *
     * Format:
     * [text_length:4][text_content][num_files:4]
     * [file1_name_length:4][file1_name][file1_data_length:4][file1_data]
     * [file2_name_length:4][file2_name][file2_data_length:4][file2_data]
     * ...
     */
    private byte[] serializeMessage(PlainMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write text content
        byte[] textBytes = message.getTextContent() != null ?
            message.getTextContent().getBytes("UTF-8") : new byte[0];

        writeInt(baos, textBytes.length);
        baos.write(textBytes);

        // Write number of files
        int numFiles = message.getFiles() != null ? message.getFiles().size() : 0;
        writeInt(baos, numFiles);

        // Write each file
        if (message.getFiles() != null) {
            for (PlainMessage.AttachedFile file : message.getFiles()) {
                // File name
                byte[] nameBytes = file.getFilename().getBytes("UTF-8");
                writeInt(baos, nameBytes.length);
                baos.write(nameBytes);

                // File data
                writeInt(baos, file.getData().length);
                baos.write(file.getData());

                // Content type
                byte[] typeBytes = file.getContentType().getBytes("UTF-8");
                writeInt(baos, typeBytes.length);
                baos.write(typeBytes);
            }
        }

        return baos.toByteArray();
    }

    /**
     * Deserialize bytes to message.
     */
    private PlainMessage deserializeMessage(byte[] data) throws IOException {
        int pos = 0;

        // Read text content
        int textLength = readInt(data, pos);
        pos += 4;

        String textContent = textLength > 0 ?
            new String(data, pos, textLength, "UTF-8") : "";
        pos += textLength;

        // Read number of files
        int numFiles = readInt(data, pos);
        pos += 4;

        // Read files
        java.util.List<PlainMessage.AttachedFile> files = new java.util.ArrayList<>();

        for (int i = 0; i < numFiles; i++) {
            // Read filename
            int nameLength = readInt(data, pos);
            pos += 4;
            String filename = new String(data, pos, nameLength, "UTF-8");
            pos += nameLength;

            // Read file data
            int dataLength = readInt(data, pos);
            pos += 4;
            byte[] fileData = new byte[dataLength];
            System.arraycopy(data, pos, fileData, 0, dataLength);
            pos += dataLength;

            // Read content type
            int typeLength = readInt(data, pos);
            pos += 4;
            String contentType = new String(data, pos, typeLength, "UTF-8");
            pos += typeLength;

            files.add(PlainMessage.AttachedFile.builder()
                .filename(filename)
                .data(fileData)
                .contentType(contentType)
                .build());
        }

        return PlainMessage.builder()
            .textContent(textContent)
            .files(files)
            .build();
    }

    private void writeInt(ByteArrayOutputStream baos, int value) {
        baos.write((value >> 24) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write(value & 0xFF);
    }

    private int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }
}
