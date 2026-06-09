package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorAssignmentResultDto;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvisorAssignmentServiceTest {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanApplicationHistoryService historyService;

    @InjectMocks
    private AdvisorAssignmentService advisorAssignmentService;

    @Test
    void assignUnassignedApplications_assignsLoansToAdvisorWithLowestWorkload() {
        User admin = adminUser();
        User advisor = advisorUser(20L, "Marie", "Conseil");
        User otherAdvisor = advisorUser(21L, "Paul", "Durand");
        LoanApplication loan = submittedLoan(1L, "LOAN-001");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(userRepository.findAllConseillers()).thenReturn(List.of(advisor, otherAdvisor));
        when(loanRepository.findUnassignedSubmittedOrderBySubmittedAtAsc()).thenReturn(List.of(loan));
        when(loanRepository.countActiveAssignments(20L)).thenReturn(1L);
        when(loanRepository.countActiveAssignments(21L)).thenReturn(3L);
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdvisorAssignmentResultDto result = advisorAssignmentService.assignUnassignedApplications(
                "admin@test.com",
                AdvisorAssignmentService.Trigger.MANUAL
        );

        assertThat(result.getAssignedCount()).isEqualTo(1);
        assertThat(result.getUnassignedRemaining()).isZero();
        assertThat(result.getAssignments()).hasSize(1);
        assertThat(result.getAssignments().get(0).getAdvisorId()).isEqualTo(20L);
        assertThat(result.getMessage()).contains("Affectation manuelle");
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.ADVISOR_ASSIGNED),
                eq(LoanEventActorType.ADMIN),
                eq("admin@test.com"),
                eq("Alice Admin"),
                eq(Map.of("advisorName", "Marie Conseil", "automatic", true))
        );
    }

    @Test
    void assignUnassignedApplications_throwsForbiddenForNonAdminOnManualTrigger() {
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientUser()));

        assertThatThrownBy(() -> advisorAssignmentService.assignUnassignedApplications(
                "client@test.com",
                AdvisorAssignmentService.Trigger.MANUAL
        ))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Accès réservé aux administrateurs.");

        verify(loanRepository, never()).findUnassignedSubmittedOrderBySubmittedAtAsc();
    }

    @Test
    void assignUnassignedApplications_throwsWhenAdminNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> advisorAssignmentService.assignUnassignedApplications(
                "unknown@test.com",
                AdvisorAssignmentService.Trigger.MANUAL
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utilisateur non trouvé");
    }

    @Test
    void assignUnassignedApplications_returnsMessageWhenNoAdvisorsAvailable() {
        when(userRepository.findAllConseillers()).thenReturn(List.of());
        when(loanRepository.findUnassignedSubmittedOrderBySubmittedAtAsc()).thenReturn(List.of(submittedLoan(1L, "LOAN-001")));

        AdvisorAssignmentResultDto result = advisorAssignmentService.assignUnassignedApplications(
                null,
                AdvisorAssignmentService.Trigger.SCHEDULED
        );

        assertThat(result.getAssignedCount()).isZero();
        assertThat(result.getAdvisorsAvailable()).isZero();
        assertThat(result.getMessage()).isEqualTo("Aucun conseiller disponible pour l'affectation.");
        verify(loanRepository, never()).save(any());
    }

    @Test
    void assignUnassignedApplications_recordsSystemEventOnScheduledTrigger() {
        User advisor = advisorUser(20L, "Marie", "Conseil");
        LoanApplication loan = submittedLoan(1L, "LOAN-002");

        when(userRepository.findAllConseillers()).thenReturn(List.of(advisor));
        when(loanRepository.findUnassignedSubmittedOrderBySubmittedAtAsc()).thenReturn(List.of(loan));
        when(loanRepository.countActiveAssignments(20L)).thenReturn(0L);
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdvisorAssignmentResultDto result = advisorAssignmentService.assignUnassignedApplications(
                null,
                AdvisorAssignmentService.Trigger.SCHEDULED
        );

        assertThat(result.getAssignedCount()).isEqualTo(1);
        assertThat(result.getMessage()).contains("Affectation automatique");
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.ADVISOR_ASSIGNED),
                eq(LoanEventActorType.SYSTEM),
                eq("system@loanlyfans"),
                eq("Affectation automatique"),
                eq(Map.of("advisorName", "Marie Conseil", "automatic", true))
        );
    }

    @Test
    void assignUnassignedApplications_returnsMessageWhenNoUnassignedLoans() {
        User advisor = advisorUser(20L, "Marie", "Conseil");

        when(userRepository.findAllConseillers()).thenReturn(List.of(advisor));
        when(loanRepository.findUnassignedSubmittedOrderBySubmittedAtAsc()).thenReturn(List.of());

        AdvisorAssignmentResultDto result = advisorAssignmentService.assignUnassignedApplications(
                null,
                AdvisorAssignmentService.Trigger.SCHEDULED
        );

        assertThat(result.getAssignedCount()).isZero();
        assertThat(result.getUnassignedRemaining()).isZero();
        assertThat(result.getMessage()).isEqualTo("Aucun dossier non affecté en attente.");
        verify(loanRepository, never()).save(any());
    }

    private static User adminUser() {
        return User.builder()
                .id(30L)
                .email("admin@test.com")
                .firstName("Alice")
                .lastName("Admin")
                .roles(Set.of(Role.builder().id(3L).name(ROLE_ADMIN).build()))
                .build();
    }

    private static User clientUser() {
        return User.builder()
                .id(10L)
                .email("client@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(Set.of(Role.builder().id(1L).name("ROLE_CLIENT").build()))
                .build();
    }

    private static User advisorUser(Long id, String firstName, String lastName) {
        return User.builder()
                .id(id)
                .email(firstName.toLowerCase() + "@test.com")
                .firstName(firstName)
                .lastName(lastName)
                .roles(Set.of(Role.builder().id(id).name(ROLE_CONSEILLER).build()))
                .build();
    }

    private static LoanApplication submittedLoan(Long id, String reference) {
        return LoanApplication.builder()
                .id(id)
                .reference(reference)
                .status(LoanApplicationStatus.SUBMITTED)
                .build();
    }
}
