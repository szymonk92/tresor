package io.tresor.api.repository;

import io.tresor.api.model.Message;
import io.tresor.api.model.Message.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Message entities.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    /**
     * Find all messages for a user.
     */
    List<Message> findByUserId(String userId);

    /**
     * Find messages by user and status.
     */
    List<Message> findByUserIdAndStatus(String userId, MessageStatus status);

    /**
     * Find messages that are ready to unlock.
     *
     * Criteria:
     * - Status is LOCKED
     * - Unlock date has passed
     * - Next check time is null or in the past
     */
    @Query("SELECT m FROM Message m WHERE " +
           "m.status = 'LOCKED' AND " +
           "m.unlockDate <= :now AND " +
           "(m.nextUnlockCheck IS NULL OR m.nextUnlockCheck <= :now)")
    List<Message> findReadyToUnlock(@Param("now") LocalDateTime now);

    /**
     * Find unlocked messages that haven't been delivered yet.
     */
    List<Message> findByStatusAndDeliveredAtIsNull(MessageStatus status);

    /**
     * Count messages by user.
     */
    long countByUserId(String userId);

    /**
     * Count messages by user and status.
     */
    long countByUserIdAndStatus(String userId, MessageStatus status);
}
