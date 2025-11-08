package io.tresor.api.service;

import io.tresor.api.model.Message;
import io.tresor.api.model.User;
import io.tresor.api.repository.MessageRepository;
import io.tresor.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService.
 */
class MessageServiceTest {

    private MessageService messageService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageService = new MessageService(messageRepository, userRepository);

        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@tresor.io");
    }

    @Test
    @DisplayName("Should create message with password encryption")
    void testCreatePasswordEncryptedMessage() throws Exception {
        // Given
        String userId = "user-123";
        String textContent = "Dear future self...";
        LocalDateTime unlockDate = LocalDateTime.now().plusYears(1);
        String password = "myPassword123";
        String passwordHint = "My favorite pet";
        String deploymentTier = "budget";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            userId,
            textContent,
            unlockDate,
            "PASSWORD_ENCRYPTION",
            password,
            passwordHint,
            deploymentTier
        );

        // Then
        assertNotNull(result);
        assertEquals("PASSWORD_ENCRYPTION", result.getEncryptionMode());
        assertEquals("budget", result.getDeploymentTier());
        assertEquals(passwordHint, result.getPasswordHint());
        assertEquals(0.50, result.getCostUsd());
        assertEquals(Message.MessageStatus.PENDING_BATCH, result.getStatus());

        verify(messageRepository, times(1)).save(any(Message.class));
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should validate password length")
    void testPasswordValidation() {
        // Given
        String shortPassword = "1234567"; // Only 7 characters

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> messageService.createMessage(
                "user-123",
                "Test",
                LocalDateTime.now().plusDays(1),
                "PASSWORD_ENCRYPTION",
                shortPassword,
                null,
                "budget"
            ),
            "Password must be at least 8 characters"
        );
    }

    @Test
    @DisplayName("Should set PENDING_BATCH status for budget tier")
    void testBudgetTierStatus() throws Exception {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            "PASSWORD_ENCRYPTION",
            "password123",
            null,
            "budget"
        );

        // Then
        assertEquals(Message.MessageStatus.PENDING_BATCH, result.getStatus());
    }

    @Test
    @DisplayName("Should set LOCKED status for premium tier")
    void testPremiumTierStatus() throws Exception {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            "PASSWORD_ENCRYPTION",
            "password123",
            null,
            "premium"
        );

        // Then
        assertEquals(Message.MessageStatus.LOCKED, result.getStatus());
    }

    @Test
    @DisplayName("Should calculate correct cost for budget tier")
    void testBudgetTierCost() throws Exception {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            "PASSWORD_ENCRYPTION",
            "password123",
            null,
            "budget"
        );

        // Then
        assertEquals(0.50, result.getCostUsd());
    }

    @Test
    @DisplayName("Should calculate correct cost for premium tier")
    void testPremiumTierCost() throws Exception {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            "FULL_ENCRYPTION",
            null,
            null,
            "premium"
        );

        // Then
        assertTrue(result.getCostUsd() >= 13.00);
    }

    @Test
    @DisplayName("Should retrieve messages by user")
    void testGetMessagesByUser() {
        // Given
        String userId = "user-123";
        Message msg1 = new Message();
        msg1.setId("msg-1");
        msg1.setUserId(userId);

        Message msg2 = new Message();
        msg2.setId("msg-2");
        msg2.setUserId(userId);

        when(messageRepository.findByUserId(userId)).thenReturn(Arrays.asList(msg1, msg2));

        // When
        List<Message> messages = messageService.getMessagesByUser(userId);

        // Then
        assertEquals(2, messages.size());
        verify(messageRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should retrieve message by ID")
    void testGetMessageById() {
        // Given
        String messageId = "msg-123";
        Message message = new Message();
        message.setId(messageId);
        message.setUserId("user-123");

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        // When
        Message result = messageService.getMessageById(messageId);

        // Then
        assertNotNull(result);
        assertEquals(messageId, result.getId());
        verify(messageRepository, times(1)).findById(messageId);
    }

    @Test
    @DisplayName("Should return null for non-existent message")
    void testGetNonExistentMessage() {
        // Given
        when(messageRepository.findById(anyString())).thenReturn(Optional.empty());

        // When
        Message result = messageService.getMessageById("non-existent");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should get user statistics")
    void testGetUserStats() {
        // Given
        String userId = "user-123";
        when(messageRepository.countByUserId(userId)).thenReturn(10L);
        when(messageRepository.countByUserIdAndStatus(userId, Message.MessageStatus.LOCKED)).thenReturn(7L);
        when(messageRepository.countByUserIdAndStatus(userId, Message.MessageStatus.UNLOCKED)).thenReturn(2L);
        when(messageRepository.countByUserIdAndStatus(userId, Message.MessageStatus.DELIVERED)).thenReturn(1L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        MessageService.MessageStatistics stats = messageService.getUserStatistics(userId);

        // Then
        assertNotNull(stats);
        assertEquals(10L, stats.totalMessages());
        assertEquals(7L, stats.lockedMessages());
        assertEquals(2L, stats.unlockedMessages());
        assertEquals(1L, stats.deliveredMessages());
    }

    @Test
    @DisplayName("Should update user total spent")
    void testUpdateUserTotalSpent() throws Exception {
        // Given
        testUser.setTotalSpentUsd(0.0);
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            "PASSWORD_ENCRYPTION",
            "password123",
            null,
            "budget"
        );

        // Then
        verify(userRepository, times(1)).save(argThat(user ->
            user.getTotalSpentUsd() > 0.0
        ));
    }

    @Test
    @DisplayName("Should throw exception for user not found")
    void testUserNotFound() {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> messageService.createMessage(
                "non-existent",
                "Test",
                LocalDateTime.now().plusDays(1),
                "PASSWORD_ENCRYPTION",
                "password123",
                null,
                "budget"
            ),
            "User not found"
        );
    }

    @Test
    @DisplayName("Should throw exception for unlock date in past")
    void testUnlockDateInPast() {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> messageService.createMessage(
                "user-123",
                "Test",
                LocalDateTime.now().minusDays(1), // Past date
                "PASSWORD_ENCRYPTION",
                "password123",
                null,
                "budget"
            ),
            "Unlock date must be in the future"
        );
    }

    @Test
    @DisplayName("Should default to budget tier if not specified")
    void testDefaultTier() throws Exception {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            "PASSWORD_ENCRYPTION",
            "password123",
            null,
            null // No tier specified
        );

        // Then
        assertEquals("budget", result.getDeploymentTier());
    }

    @Test
    @DisplayName("Should default to PASSWORD_ENCRYPTION if not specified")
    void testDefaultEncryptionMode() throws Exception {
        // Given
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Message result = messageService.createMessage(
            "user-123",
            "Test",
            LocalDateTime.now().plusDays(1),
            null, // No mode specified
            "password123",
            null,
            "budget"
        );

        // Then
        assertEquals("PASSWORD_ENCRYPTION", result.getEncryptionMode());
    }
}
