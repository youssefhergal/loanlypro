package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.LearningSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSessionDto {
    private Long id;
    private String objective;
    private List<LearningStepDto> steps;
    private Set<Integer> completedSteps;
    private LearningSession.SessionStatus status;
    private int completedCount;
    private int totalSteps;
    private Instant createdAt;
    private Instant updatedAt;
}
