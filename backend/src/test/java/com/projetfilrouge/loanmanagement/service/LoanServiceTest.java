package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocument;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private LoanDocumentRepository loanDocumentRepository;

    @Mock
    private LoanApplicationEventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanApplicationHistoryService historyService;

    @Mock
    private DocumentReviewService documentReviewService;

    @Mock
    private LoanDocumentStorageService documentStorage;

    @InjectMocks
    private LoanService loanService;

    @Test
    void createApplication_createsDraftLoanForClient() {
        User client = clientUser();
        LoanRequestDto request = sampleLoanRequest();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.existsByReference(anyString())).thenReturn(false);
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> {
            LoanApplication saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LoanResponseDto response = loanService.createApplication(request, "client@test.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.DRAFT);
        assertThat(response.getRequestedAmount()).isEqualByComparingTo("15000");
        assertThat(response.getApplicantEmail()).isEqualTo("client@test.com");

        ArgumentCaptor<LoanApplication> loanCaptor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanRepository).save(loanCaptor.capture());
        assertThat(loanCaptor.getValue().getReference()).startsWith("LOAN-");
        assertThat(loanCaptor.getValue().getApplicant()).isEqualTo(client);

        verify(historyService).recordEvent(
                any(LoanApplication.class),
                eq(LoanApplicationEventType.APPLICATION_CREATED),
                eq(LoanEventActorType.CLIENT),
                eq("client@test.com"),
                eq("Jean Dupont"),
                any(Map.class)
        );
    }

    @Test
    void submitApplication_changesStatusToSubmittedWhenDocumentsComplete() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(requiredDocuments());
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.submitApplication(1L, "client@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.SUBMITTED);
        assertThat(response.getSubmittedAt()).isNotNull();
        verify(documentReviewService).initializePendingReviews(loan);
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.APPLICATION_SUBMITTED),
                eq(LoanEventActorType.CLIENT),
                eq("client@test.com"),
                eq("Jean Dupont"),
                any(Map.class),
                any()
        );
    }

    @Test
    void submitApplication_throwsWhenLoanIsNotDraft() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.SUBMITTED);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.submitApplication(1L, "client@test.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("brouillon");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void submitApplication_throwsWhenRequiredDocumentIsMissing() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(documentOf(LoanDocumentType.IDENTITY)));

        assertThatThrownBy(() -> loanService.submitApplication(1L, "client@test.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PAYSLIPS");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void startReview_movesSubmittedLoanToUnderReviewAndAssignsAdvisor() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.SUBMITTED);

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.startReview(1L, "conseiller@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
        assertThat(response.getAdvisorId()).isEqualTo(20L);
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.ADVISOR_ASSIGNED),
                eq(LoanEventActorType.ADVISOR),
                eq("conseiller@test.com"),
                eq("Marie Conseil"),
                any(Map.class)
        );
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.REVIEW_STARTED),
                eq(LoanEventActorType.ADVISOR),
                eq("conseiller@test.com"),
                eq("Marie Conseil"),
                any(Map.class)
        );
    }

    @Test
    void startReview_throwsForbiddenForClient() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.SUBMITTED);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.startReview(1L, "client@test.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Accès refusé");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void approveApplication_approvesLoanWithSystemOffer() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(requiredDocuments());
        doNothing().when(documentReviewService)
                .ensureRequiredDocumentsReadyForDecision(1L, "Approbation impossible");
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.approveApplication(1L, "conseiller@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("15000");
        assertThat(response.getApprovedDurationMonths()).isEqualTo(48);
        assertThat(response.getInterestRate()).isEqualByComparingTo("3.85");
        assertThat(response.getDecidedAt()).isNotNull();
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.APPLICATION_APPROVED),
                eq(LoanEventActorType.ADVISOR),
                eq("conseiller@test.com"),
                eq("Marie Conseil"),
                any(Map.class)
        );
    }

    @Test
    void rejectApplication_rejectsLoanWithComment() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);
        RejectLoanRequestDto request = RejectLoanRequestDto.builder()
                .comment("Dossier incomplet")
                .build();

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.rejectApplication(1L, request, "conseiller@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);
        assertThat(response.getDecisionComment()).isEqualTo("Dossier incomplet");
        assertThat(response.getDecidedAt()).isNotNull();
        verify(historyService).recordEvent(
                loan,
                LoanApplicationEventType.APPLICATION_REJECTED,
                LoanEventActorType.ADVISOR,
                "conseiller@test.com",
                "Marie Conseil",
                Map.of("comment", "Dossier incomplet")
        );
    }

    @Test
    void cancelApplication_cancelsSubmittedLoanForApplicant() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.SUBMITTED);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.cancelApplication(1L, null, "client@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.CANCELLED);
        assertThat(response.getDecisionComment()).isEqualTo("Annulée à la demande du client.");
        verify(historyService).recordEvent(
                loan,
                LoanApplicationEventType.APPLICATION_CANCELLED,
                LoanEventActorType.CLIENT,
                "client@test.com",
                "Jean Dupont",
                Map.of("comment", "Annulée à la demande du client.")
        );
    }

    @Test
    void getApplicationById_throwsForbiddenWhenClientAccessesAnotherUsersLoan() {
        User client = clientUser();
        User otherClient = User.builder()
                .id(99L)
                .email("other@test.com")
                .firstName("Paul")
                .lastName("Martin")
                .roles(Set.of(Role.builder().name(ROLE_CLIENT).build()))
                .build();
        LoanApplication loan = completeDraftLoan(otherClient);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.getApplicationById(1L, "client@test.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Accès refusé à cette demande");
    }

    private static LoanRequestDto sampleLoanRequest() {
        return LoanRequestDto.builder()
                .title("Prêt véhicule")
                .loanPurpose(LoanPurpose.VEHICLE)
                .requestedAmount(new BigDecimal("15000"))
                .requestedDurationMonths(48)
                .monthlyIncome(new BigDecimal("2800"))
                .employmentStatus(EmploymentStatus.CDI)
                .employerName("ACME")
                .build();
    }

    private static LoanApplication completeDraftLoan(User applicant) {
        return LoanApplication.builder()
                .id(1L)
                .reference("LOAN-TESTREF")
                .applicant(applicant)
                .status(LoanApplicationStatus.DRAFT)
                .title("Prêt véhicule")
                .loanPurpose(LoanPurpose.VEHICLE)
                .purpose("Véhicule")
                .requestedAmount(new BigDecimal("15000"))
                .requestedDurationMonths(48)
                .monthlyIncome(new BigDecimal("2800"))
                .employmentStatus(EmploymentStatus.CDI)
                .employerName("ACME")
                .build();
    }

    private static List<LoanDocument> requiredDocuments() {
        return List.of(
                documentOf(LoanDocumentType.IDENTITY),
                documentOf(LoanDocumentType.PAYSLIPS),
                documentOf(LoanDocumentType.TAX_NOTICE),
                documentOf(LoanDocumentType.BANK_STATEMENTS),
                documentOf(LoanDocumentType.PROOF_OF_ADDRESS)
        );
    }

    private static LoanDocument documentOf(LoanDocumentType type) {
        return LoanDocument.builder().documentType(type).build();
    }

    private static User clientUser() {
        return User.builder()
                .id(10L)
                .email("client@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(Set.of(Role.builder().id(1L).name(ROLE_CLIENT).build()))
                .build();
    }

    private static User advisorUser() {
        return User.builder()
                .id(20L)
                .email("conseiller@test.com")
                .firstName("Marie")
                .lastName("Conseil")
                .roles(Set.of(Role.builder().id(2L).name(ROLE_CONSEILLER).build()))
                .build();
    }
}
