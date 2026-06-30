package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.participant1
            JOIN FETCH c.participant2
            WHERE c.participant1.id = :uid1 AND c.participant2.id = :uid2
            """)
    Optional<Conversation> findByParticipants(@Param("uid1") Long uid1, @Param("uid2") Long uid2);

    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.participant1
            JOIN FETCH c.participant2
            WHERE c.participant1.id = :userId OR c.participant2.id = :userId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> findAllByParticipant(@Param("userId") Long userId);
}
