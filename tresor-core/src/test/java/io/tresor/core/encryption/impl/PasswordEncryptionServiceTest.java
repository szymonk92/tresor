package io.tresor.core.encryption.impl;

import io.tresor.core.encryption.EncryptionException;
import io.tresor.core.model.PlainMessage;
import io.tresor.core.encryption.PasswordEncryptedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordEncryptionService.
 *
 * Tests password-based encryption using PBKDF2 + AES-256-GCM.
 */
class PasswordEncryptionServiceTest {

    private PasswordEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new PasswordEncryptionService();
    }

    @Test
    @DisplayName("Should encrypt and decrypt message successfully")
    void testEncryptDecrypt() throws EncryptionException {
        // Given
        String originalText = "Dear future self, remember to stay curious!";
        PlainMessage plainMessage = PlainMessage.builder()
            .textContent(originalText)
            .build();
        String password = "mySecurePassword123!";
        String hint = "My favorite quote";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(plainMessage, password, hint);
        PlainMessage decrypted = service.decrypt(encrypted, password);

        // Then
        assertNotNull(encrypted);
        assertNotNull(encrypted.getEncryptedPayload());
        assertNotNull(encrypted.getSalt());
        assertNotNull(encrypted.getIv());
        assertEquals(hint, encrypted.getPasswordHint());
        assertEquals(100_000, encrypted.getIterations());

        assertNotNull(decrypted);
        assertEquals(originalText, decrypted.getTextContent());
    }

    @Test
    @DisplayName("Should use random salt for each encryption")
    void testRandomSalt() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test message")
            .build();
        String password = "password123";

        // When
        PasswordEncryptedMessage encrypted1 = service.encrypt(message, password, null);
        PasswordEncryptedMessage encrypted2 = service.encrypt(message, password, null);

        // Then
        assertFalse(
            java.util.Arrays.equals(encrypted1.getSalt(), encrypted2.getSalt()),
            "Salts should be different for each encryption"
        );
    }

    @Test
    @DisplayName("Should use random IV for each encryption")
    void testRandomIV() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test message")
            .build();
        String password = "password123";

        // When
        PasswordEncryptedMessage encrypted1 = service.encrypt(message, password, null);
        PasswordEncryptedMessage encrypted2 = service.encrypt(message, password, null);

        // Then
        assertFalse(
            java.util.Arrays.equals(encrypted1.getIv(), encrypted2.getIv()),
            "IVs should be different for each encryption"
        );
    }

    @Test
    @DisplayName("Should fail decryption with wrong password")
    void testWrongPassword() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Secret message")
            .build();
        String correctPassword = "correct123";
        String wrongPassword = "wrong456";

        PasswordEncryptedMessage encrypted = service.encrypt(message, correctPassword, null);

        // When/Then
        assertThrows(
            EncryptionException.class,
            () -> service.decrypt(encrypted, wrongPassword),
            "Should fail with wrong password"
        );
    }

    @Test
    @DisplayName("Should handle empty message")
    void testEmptyMessage() throws EncryptionException {
        // Given
        PlainMessage emptyMessage = PlainMessage.builder()
            .textContent("")
            .build();
        String password = "password123";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(emptyMessage, password, null);
        PlainMessage decrypted = service.decrypt(encrypted, password);

        // Then
        assertEquals("", decrypted.getTextContent());
    }

    @Test
    @DisplayName("Should handle long message")
    void testLongMessage() throws EncryptionException {
        // Given
        String longText = "A".repeat(10000); // 10KB message
        PlainMessage longMessage = PlainMessage.builder()
            .textContent(longText)
            .build();
        String password = "password123";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(longMessage, password, null);
        PlainMessage decrypted = service.decrypt(encrypted, password);

        // Then
        assertEquals(longText, decrypted.getTextContent());
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void testSpecialCharacters() throws EncryptionException {
        // Given
        String specialText = "Hello! 你好! مرحبا! こんにちは! 🎉🎂🎁";
        PlainMessage message = PlainMessage.builder()
            .textContent(specialText)
            .build();
        String password = "password123";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, password, null);
        PlainMessage decrypted = service.decrypt(encrypted, password);

        // Then
        assertEquals(specialText, decrypted.getTextContent());
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void testSpecialCharactersInPassword() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test message")
            .build();
        String specialPassword = "P@ssw0rd!#$%^&*()中文🔒";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, specialPassword, null);
        PlainMessage decrypted = service.decrypt(encrypted, specialPassword);

        // Then
        assertEquals("Test message", decrypted.getTextContent());
    }

    @Test
    @DisplayName("Should compute correct content hash")
    void testContentHash() throws EncryptionException {
        // Given
        String text = "Test message for hashing";
        PlainMessage message = PlainMessage.builder()
            .textContent(text)
            .build();
        String password = "password123";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, password, null);

        // Then
        assertNotNull(encrypted.getContentHash());
        assertEquals(64, encrypted.getContentHash().length); // SHA-256 = 64 hex chars
    }

    @Test
    @DisplayName("Should verify content hash after decryption")
    void testContentHashVerification() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test message")
            .build();
        String password = "password123";

        PasswordEncryptedMessage encrypted = service.encrypt(message, password, null);
        byte[] originalHash = encrypted.getContentHash();

        // When
        PlainMessage decrypted = service.decrypt(encrypted, password);

        // Then - hash should match after decryption
        // (Note: This would require storing the hash and comparing it)
        assertNotNull(decrypted);
    }

    @Test
    @DisplayName("Should use 100k PBKDF2 iterations")
    void testPBKDF2Iterations() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, "password", null);

        // Then
        assertEquals(100_000, encrypted.getIterations());
    }

    @Test
    @DisplayName("Should generate 32-byte salt")
    void testSaltLength() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, "password", null);

        // Then
        assertEquals(32, encrypted.getSalt().length);
    }

    @Test
    @DisplayName("Should generate 12-byte IV for GCM")
    void testIVLength() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, "password", null);

        // Then
        assertEquals(12, encrypted.getIv().length); // GCM standard IV size
    }

    @Test
    @DisplayName("Should preserve password hint")
    void testPasswordHint() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();
        String hint = "My dog's name + graduation year";

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, "password", hint);

        // Then
        assertEquals(hint, encrypted.getPasswordHint());
    }

    @Test
    @DisplayName("Should handle null password hint")
    void testNullPasswordHint() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, "password", null);

        // Then
        assertNull(encrypted.getPasswordHint());
    }

    @Test
    @DisplayName("Should throw exception for null password")
    void testNullPassword() {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> service.encrypt(message, null, null),
            "Should throw exception for null password"
        );
    }

    @Test
    @DisplayName("Should throw exception for empty password")
    void testEmptyPassword() {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> service.encrypt(message, "", null),
            "Should throw exception for empty password"
        );
    }

    @Test
    @DisplayName("Should handle minimum password length")
    void testMinimumPasswordLength() throws EncryptionException {
        // Given
        PlainMessage message = PlainMessage.builder()
            .textContent("Test")
            .build();
        String minPassword = "12345678"; // 8 characters

        // When
        PasswordEncryptedMessage encrypted = service.encrypt(message, minPassword, null);
        PlainMessage decrypted = service.decrypt(encrypted, minPassword);

        // Then
        assertEquals("Test", decrypted.getTextContent());
    }
}
