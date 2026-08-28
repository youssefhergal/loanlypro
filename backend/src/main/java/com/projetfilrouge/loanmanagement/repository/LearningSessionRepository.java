package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    List<LearningSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LearningSession> findByIdAndUserId(Long id, Long userId);
}
