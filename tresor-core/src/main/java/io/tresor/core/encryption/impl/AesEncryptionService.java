package io.tresor.core.encryption.impl;

import io.tresor.core.encryption.EncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Service for encrypting and decrypting message content using AES-256-GCM.
 *
 * AES-256-GCM provides:
 * - Confidentiality (encryption)
 * - Authenticity (prevents tampering)
 * - Associated data (can authenticate metadata without encrypting it)
 */
@Slf4j
public class AesEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // bits
    private static final int GCM_TAG_LENGTH = 128; // bits (16 bytes)
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits - NIST recommended)

    private final SecureRandom secureRandom;

    static {
        // Register BouncyCastle as security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public AesEncryptionService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a random AES-256 key.
     */
    public SecretKey generateKey() throws EncryptionException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE, secureRandom);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to generate AES key", e);
        }
    }

    /**
     * Generate a random Initialization Vector (IV) for GCM mode.
     */
    public byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt plaintext data with AES-256-GCM.
     *
     * @param plaintext The data to encrypt
     * @param key The AES-256 key
     * @param iv The initialization vector (must be unique per encryption)
     * @return Encrypted ciphertext (includes authentication tag)
     */
    public byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws EncryptionException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt ciphertext with AES-256-GCM.
     *
     * @param ciphertext The encrypted data
     * @param key The AES-256 key
     * @param iv The initialization vector used during encryption
     * @return Decrypted plaintext
     */
    public byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws EncryptionException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt data - possible tampering or wrong key", e);
        }
    }

    /**
     * Convert a raw byte array to a SecretKey.
     */
    public SecretKey bytesToKey(byte[] keyBytes) {
        if (keyBytes.length != KEY_SIZE / 8) {
            throw new IllegalArgumentException("Key must be " + KEY_SIZE + " bits (" + (KEY_SIZE / 8) + " bytes)");
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Convert a SecretKey to a byte array.
     */
    public byte[] keyToBytes(SecretKey key) {
        return key.getEncoded();
    }
}
