package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorAssignmentItemDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorAssignmentResultDto;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdvisorAssignmentService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String SYSTEM_EMAIL = "system@loanlyfans";

    private final LoanApplicationRepository loanRepository;
    private final UserRepository userRepository;
    private final LoanApplicationHistoryService historyService;

    public enum Trigger {
        SCHEDULED,
        MANUAL
    }

    @Transactional
    public AdvisorAssignmentResultDto assignUnassignedApplications(String currentUserEmail, Trigger trigger) {
        User admin = null;
        if (trigger == Trigger.MANUAL) {
            admin = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
            if (!hasRole(admin, ROLE_ADMIN)) {
                throw new ForbiddenOperationException("Accès réservé aux administrateurs.");
            }
        }

        List<User> advisors = userRepository.findAllConseillers();
        if (advisors.isEmpty()) {
            return AdvisorAssignmentResultDto.builder()
                    .assignedCount(0)
                    .unassignedRemaining(countUnassignedSubmitted())
                    .advisorsAvailable(0)
                    .message("Aucun conseiller disponible pour l'affectation.")
                    .assignments(List.of())
                    .build();
        }

        List<LoanApplication> unassigned = loanRepository.findUnassignedSubmittedOrderBySubmittedAtAsc();
        if (unassigned.isEmpty()) {
            return AdvisorAssignmentResultDto.builder()
                    .assignedCount(0)
                    .unassignedRemaining(0)
                    .advisorsAvailable(advisors.size())
                    .message("Aucun dossier non affecté en attente.")
                    .assignments(List.of())
                    .build();
        }

        Map<Long, Long> workload = new HashMap<>();
        for (User advisor : advisors) {
            workload.put(advisor.getId(), loanRepository.countActiveAssignments(advisor.getId()));
        }

        List<AdvisorAssignmentItemDto> assignments = new ArrayList<>();
        for (LoanApplication loan : unassigned) {
            User chosen = pickAdvisorWithLowestWorkload(advisors, workload);
            loan.setAssignedAdvisor(chosen);
            LoanApplication saved = loanRepository.save(loan);
            workload.merge(chosen.getId(), 1L, Long::sum);

            recordAssignment(saved, chosen, trigger, admin);
            assignments.add(AdvisorAssignmentItemDto.builder()
                    .loanId(saved.getId())
                    .reference(saved.getReference())
                    .advisorId(chosen.getId())
                    .advisorName(displayName(chosen))
                    .build());
        }

        return AdvisorAssignmentResultDto.builder()
                .assignedCount(assignments.size())
                .unassignedRemaining(0)
                .advisorsAvailable(advisors.size())
                .message(buildSuccessMessage(assignments.size(), trigger))
                .assignments(assignments)
                .build();
    }

    private void recordAssignment(
            LoanApplication loan,
            User advisor,
            Trigger trigger,
            User admin
    ) {
        if (trigger == Trigger.MANUAL && admin != null) {
            historyService.recordEvent(
                    loan,
                    LoanApplicationEventType.ADVISOR_ASSIGNED,
                    LoanEventActorType.ADMIN,
                    admin.getEmail(),
                    displayName(admin),
                    Map.of(
                            "advisorName", displayName(advisor),
                            "automatic", true
                    )
            );
            return;
        }

        historyService.recordEvent(
                loan,
                LoanApplicationEventType.ADVISOR_ASSIGNED,
                LoanEventActorType.SYSTEM,
                SYSTEM_EMAIL,
                "Affectation automatique",
                Map.of(
                        "advisorName", displayName(advisor),
                        "automatic", true
                )
        );
    }

    private User pickAdvisorWithLowestWorkload(List<User> advisors, Map<Long, Long> workload) {
        return advisors.stream()
                .min(Comparator
                        .comparing((User advisor) -> workload.getOrDefault(advisor.getId(), 0L))
                        .thenComparing(User::getId))
                .orElseThrow();
    }

    private int countUnassignedSubmitted() {
        return loanRepository.findUnassignedSubmittedOrderBySubmittedAtAsc().size();
    }

    private String buildSuccessMessage(int assignedCount, Trigger trigger) {
        String source = trigger == Trigger.MANUAL ? "Affectation manuelle" : "Affectation automatique";
        return source + " : " + assignedCount + " dossier(s) assigné(s).";
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private String displayName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
