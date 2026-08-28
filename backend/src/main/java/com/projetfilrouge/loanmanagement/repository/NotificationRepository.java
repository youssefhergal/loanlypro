package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    long countByRecipientId(Long recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.id = :id AND n.recipient.id = :recipientId AND n.readAt IS NULL
            """)
    int markAsRead(@Param("id") Long id, @Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.recipient.id = :recipientId AND n.readAt IS NULL
            """)
    int markAllAsRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);
}
