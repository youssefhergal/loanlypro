package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            WHERE m.conversation.id = :convId
            ORDER BY m.sentAt ASC
            """)
    List<Message> findByConversationIdOrderBySentAtAsc(@Param("convId") Long convId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :convId
              AND m.sender.id != :userId
              AND m.readAt IS NULL
            """)
    long countUnread(@Param("convId") Long convId, @Param("userId") Long userId);

    @Query("""
            SELECT SUM(
              (SELECT COUNT(m) FROM Message m
               WHERE m.conversation = c
                 AND m.sender.id != :userId
                 AND m.readAt IS NULL)
            )
            FROM Conversation c
            WHERE c.participant1.id = :userId OR c.participant2.id = :userId
            """)
    Long countTotalUnread(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE Message m SET m.readAt = :now
            WHERE m.conversation.id = :convId
              AND m.sender.id != :userId
              AND m.readAt IS NULL
            """)
    int markAllReadInConversation(
            @Param("convId") Long convId,
            @Param("userId") Long userId,
            @Param("now") Instant now);
}
