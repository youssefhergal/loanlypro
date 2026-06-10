package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
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
import com.projetfilrouge.loanmanagement.web.dto.request.AdminApplicationListQuery;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanSubmittedUpdateDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ProposeOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ValidateDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdminLoanListSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentReviewResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
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
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

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
    void getAdminApplications_returnsPaginatedResultsForAdmin() {
        User admin = adminUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.SUBMITTED);
        AdminApplicationListQuery query = new AdminApplicationListQuery(null, null, false, null, null, 0, 10);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(loanRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        Page<LoanResponseDto> result = loanService.getAdminApplications("admin@test.com", query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(LoanApplicationStatus.SUBMITTED);
    }

    @Test
    void getAdminApplications_throwsForbiddenForNonAdmin() {
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientUser()));
        AdminApplicationListQuery query = new AdminApplicationListQuery(null, null, false, null, null, 0, 10);

        assertThatThrownBy(() -> loanService.getAdminApplications("client@test.com", query))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Accès réservé aux administrateurs.");
    }

    @Test
    void getAdminListSummary_returnsCountsAndAdvisors() {
        User admin = adminUser();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(loanRepository.count(any(Specification.class))).thenReturn(5L, 2L, 1L, 1L, 1L, 1L, 1L, 1L);
        when(userRepository.findAllConseillers()).thenReturn(List.of(advisorUser()));

        AdminLoanListSummaryDto summary = loanService.getAdminListSummary("admin@test.com", null);

        assertThat(summary.getTotalCount()).isEqualTo(5L);
        assertThat(summary.getUnassignedCount()).isEqualTo(2L);
        assertThat(summary.getAdvisors()).hasSize(1);
        assertThat(summary.getStatusCounts()).isNotEmpty();
    }

    @Test
    void proposeCounterOffer_setsOfferPendingStatus() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);
        ProposeOfferRequestDto request = new ProposeOfferRequestDto();
        request.setApprovedAmount(new BigDecimal("12000"));
        request.setApprovedDurationMonths(36);
        request.setInterestRate(new BigDecimal("4.50"));
        request.setClientMessage("  Offre adaptée  ");

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(requiredDocuments());
        doNothing().when(documentReviewService)
                .ensureRequiredDocumentsReadyForDecision(1L, "Contre-offre impossible");
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.proposeCounterOffer(1L, request, "conseiller@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.OFFER_PENDING);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("12000");
        assertThat(response.getOfferMessage()).isEqualTo("Offre adaptée");
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.OFFER_PROPOSED),
                eq(LoanEventActorType.ADVISOR),
                eq("conseiller@test.com"),
                eq("Marie Conseil"),
                any(Map.class)
        );
    }

    @Test
    void acceptOffer_movesLoanBackToUnderReview() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.OFFER_PENDING);
        loan.setApprovedAmount(new BigDecimal("12000"));
        loan.setApprovedDurationMonths(36);
        loan.setInterestRate(new BigDecimal("4.50"));

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.acceptOffer(1L, "client@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
        assertThat(response.getOfferClientAccepted()).isTrue();
        verify(historyService).recordEvent(
                loan,
                LoanApplicationEventType.OFFER_ACCEPTED,
                LoanEventActorType.CLIENT,
                "client@test.com",
                "Jean Dupont",
                Map.of()
        );
    }

    @Test
    void rejectOffer_clearsApprovedTerms() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.OFFER_PENDING);
        loan.setApprovedAmount(new BigDecimal("12000"));
        loan.setApprovedDurationMonths(36);
        loan.setInterestRate(new BigDecimal("4.50"));
        loan.setOfferMessage("Offre");
        RejectOfferRequestDto request = new RejectOfferRequestDto();
        request.setComment("Trop cher");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.rejectOffer(1L, request, "client@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
        assertThat(response.getApprovedAmount()).isNull();
        assertThat(response.getOfferMessage()).isNull();
        verify(historyService).recordEvent(
                loan,
                LoanApplicationEventType.OFFER_REJECTED,
                LoanEventActorType.CLIENT,
                "client@test.com",
                "Jean Dupont",
                Map.of("comment", "Trop cher")
        );
    }

    @Test
    void uploadDocument_storesFileInDraftLoan() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        MultipartFile file = new MockMultipartFile(
                "file",
                "identity.pdf",
                "application/pdf",
                "pdf".getBytes()
        );
        LoanDocumentStorageService.StoredUpload stored = new LoanDocumentStorageService.StoredUpload(
                "uuid-identity.pdf",
                "/tmp/loan-1/uuid-identity.pdf",
                "identity.pdf",
                "application/pdf",
                3L
        );

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(documentStorage.storeUpload(1L, file)).thenReturn(stored);
        when(loanDocumentRepository.save(any(LoanDocument.class))).thenAnswer(invocation -> {
            LoanDocument doc = invocation.getArgument(0);
            doc.setId(50L);
            return doc;
        });

        LoanDocumentResponseDto response = loanService.uploadDocument(
                1L,
                LoanDocumentType.IDENTITY,
                file,
                null,
                "client@test.com"
        );

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getDocumentType()).isEqualTo(LoanDocumentType.IDENTITY);
        assertThat(response.getOriginalFileName()).isEqualTo("identity.pdf");
    }

    @Test
    void getAllApplications_returnsClientLoans() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findByApplicantEmail(eq("client@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        Page<LoanResponseDto> result = loanService.getAllApplications("client@test.com", null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getApplicantEmail()).isEqualTo("client@test.com");
    }

    @Test
    void deleteApplication_deletesDraftForClient() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        LoanDocument document = LoanDocument.builder()
                .id(5L)
                .storagePath("/tmp/doc.pdf")
                .build();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(document));

        loanService.deleteApplication(1L, "client@test.com");

        verify(documentStorage).deleteFile("/tmp/doc.pdf");
        verify(loanDocumentRepository).deleteAll(List.of(document));
        verify(documentStorage).deleteLoanStorageDirectory(1L);
        verify(eventRepository).deleteByLoanApplicationId(1L);
        verify(loanRepository).delete(loan);
    }

    @Test
    void deleteApplication_throwsWhenClientDeletesNonDraft() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.SUBMITTED);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.deleteApplication(1L, "client@test.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("brouillon");

        verify(loanRepository, never()).delete(any(LoanApplication.class));
    }

    @Test
    void getApplicationById_returnsLoanForApplicant() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        LoanResponseDto response = loanService.getApplicationById(1L, "client@test.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getApplicantEmail()).isEqualTo("client@test.com");
    }

    @Test
    void updateDraftApplication_updatesFields() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        LoanRequestDto request = sampleLoanRequest();
        request.setTitle("Nouveau titre");
        request.setRequestedAmount(new BigDecimal("20000"));

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.updateDraftApplication(1L, request, "client@test.com");

        assertThat(response.getTitle()).isEqualTo("Nouveau titre");
        assertThat(response.getRequestedAmount()).isEqualByComparingTo("20000");
    }

    @Test
    void updateSubmittedApplication_assignsAdvisor() {
        User admin = adminUser();
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.SUBMITTED);
        LoanSubmittedUpdateDto request = new LoanSubmittedUpdateDto();
        request.setAssignedAdvisorId(20L);
        request.setApprovedAmount(new BigDecimal("14000"));

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(userRepository.findById(20L)).thenReturn(Optional.of(advisor));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponseDto response = loanService.updateSubmittedApplication(1L, request, "admin@test.com");

        assertThat(response.getAdvisorId()).isEqualTo(20L);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("14000");
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.ADVISOR_ASSIGNED),
                eq(LoanEventActorType.ADMIN),
                eq("admin@test.com"),
                eq("Alice Admin"),
                any(Map.class)
        );
    }

    @Test
    void startReview_returnsUnchangedWhenAlreadyUnderReview() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        LoanResponseDto response = loanService.startReview(1L, "conseiller@test.com");

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
        verify(loanRepository, never()).save(any(LoanApplication.class));
    }

    @Test
    void validateDocument_recordsHistoryEvent() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);
        ValidateDocumentRequestDto request = ValidateDocumentRequestDto.builder()
                .documentType(LoanDocumentType.IDENTITY)
                .build();
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .id(77L)
                .loanApplication(loan)
                .eventType(LoanApplicationEventType.DOCUMENT_VALIDATED)
                .build();
        LoanHistoryEventResponseDto dto = LoanHistoryEventResponseDto.builder()
                .id(77L)
                .title("Document validé")
                .build();

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(documentOf(LoanDocumentType.IDENTITY)));
        when(historyService.recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.DOCUMENT_VALIDATED),
                eq(LoanEventActorType.ADVISOR),
                eq("conseiller@test.com"),
                eq("Marie Conseil"),
                any(Map.class)
        )).thenReturn(event);
        when(historyService.mapEventToDisplayDto(event)).thenReturn(dto);

        LoanHistoryEventResponseDto response = loanService.validateDocument(1L, request, "conseiller@test.com");

        assertThat(response.getTitle()).isEqualTo("Document validé");
        verify(documentReviewService).markValidated(loan, LoanDocumentType.IDENTITY);
    }

    @Test
    void rejectDocument_recordsHistoryEvent() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);
        RejectDocumentRequestDto request = RejectDocumentRequestDto.builder()
                .documentType(LoanDocumentType.PAYSLIPS)
                .comment("Mois manquants")
                .build();
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .id(78L)
                .loanApplication(loan)
                .eventType(LoanApplicationEventType.DOCUMENT_REJECTED)
                .build();

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(historyService.recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.DOCUMENT_REJECTED),
                eq(LoanEventActorType.ADVISOR),
                eq("conseiller@test.com"),
                eq("Marie Conseil"),
                any(Map.class)
        )).thenReturn(event);
        when(historyService.mapEventToDisplayDto(event)).thenReturn(
                LoanHistoryEventResponseDto.builder().title("Complément demandé").build()
        );

        LoanHistoryEventResponseDto response = loanService.rejectDocument(1L, request, "conseiller@test.com");

        assertThat(response.getTitle()).isEqualTo("Complément demandé");
        verify(documentReviewService).markRejected(loan, LoanDocumentType.PAYSLIPS, "Mois manquants");
    }

    @Test
    void getApplicationHistory_returnsTimeline() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(historyService.getHistory(1L)).thenReturn(List.of(
                LoanHistoryEventResponseDto.builder().title("Brouillon créé").build()
        ));

        List<LoanHistoryEventResponseDto> history = loanService.getApplicationHistory(1L, "client@test.com");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getTitle()).isEqualTo("Brouillon créé");
    }

    @Test
    void getDocuments_returnsUploadedFiles() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        LoanDocument document = LoanDocument.builder()
                .id(5L)
                .loanApplication(loan)
                .documentType(LoanDocumentType.IDENTITY)
                .originalFileName("id.pdf")
                .contentType("application/pdf")
                .fileSizeBytes(100L)
                .uploadedAt(Instant.parse("2026-01-10T10:00:00Z"))
                .build();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(document));

        List<LoanDocumentResponseDto> documents = loanService.getDocuments(1L, "client@test.com");

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getOriginalFileName()).isEqualTo("id.pdf");
    }

    @Test
    void getDocumentReviews_delegatesToDocumentReviewService() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        List<LoanDocumentReviewResponseDto> reviews = List.of(
                LoanDocumentReviewResponseDto.builder()
                        .documentType(LoanDocumentType.IDENTITY)
                        .status("pending_review")
                        .build()
        );

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(documentReviewService.getDocumentReviews(loan)).thenReturn(reviews);

        List<LoanDocumentReviewResponseDto> result = loanService.getDocumentReviews(1L, "client@test.com");

        assertThat(result).isEqualTo(reviews);
    }

    @Test
    void uploadComplementDocument_storesFileAndRecordsHistory() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        MultipartFile file = new MockMultipartFile(
                "file",
                "extra.pdf",
                "application/pdf",
                "extra".getBytes()
        );
        LoanDocumentStorageService.StoredUpload stored = new LoanDocumentStorageService.StoredUpload(
                "uuid-extra.pdf",
                "/tmp/loan-1/uuid-extra.pdf",
                "extra.pdf",
                "application/pdf",
                5L
        );

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(documentStorage.storeUpload(1L, file)).thenReturn(stored);
        when(loanDocumentRepository.save(any(LoanDocument.class))).thenAnswer(invocation -> {
            LoanDocument doc = invocation.getArgument(0);
            doc.setId(60L);
            return doc;
        });

        LoanDocumentResponseDto response = loanService.uploadComplementDocument(
                1L,
                LoanDocumentType.OTHER,
                file,
                "Justificatif complémentaire",
                "client@test.com"
        );

        assertThat(response.getDisplayName()).isEqualTo("Justificatif complémentaire");
        verify(documentReviewService).markPendingReview(loan, LoanDocumentType.OTHER);
        verify(historyService).recordEvent(
                eq(loan),
                eq(LoanApplicationEventType.DOCUMENT_UPLOADED),
                eq(LoanEventActorType.CLIENT),
                eq("client@test.com"),
                eq("Jean Dupont"),
                any(Map.class)
        );
    }

    @Test
    void deleteDocument_removesFileFromDraft() {
        User client = clientUser();
        LoanApplication loan = completeDraftLoan(client);
        LoanDocument document = LoanDocument.builder()
                .id(8L)
                .storedFileName("stored.pdf")
                .storagePath("/tmp/stored.pdf")
                .build();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByIdAndLoanApplicationId(8L, 1L)).thenReturn(Optional.of(document));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of());

        loanService.deleteDocument(1L, 8L, "client@test.com");

        verify(documentStorage).deleteFile("/tmp/stored.pdf");
        verify(loanDocumentRepository).delete(document);
        verify(documentStorage).cleanupLoanDirectoryOrphans(1L, Set.of());
    }

    @Test
    void downloadDocument_returnsFileForAdvisor() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);
        LoanDocument document = LoanDocument.builder()
                .id(9L)
                .originalFileName("id.pdf")
                .storagePath("/tmp/id.pdf")
                .contentType("application/pdf")
                .build();
        LoanDocumentStorageService.DownloadedFile downloaded =
                new LoanDocumentStorageService.DownloadedFile("id.pdf", "application/pdf", new byte[] {1});

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByIdAndLoanApplicationId(9L, 1L)).thenReturn(Optional.of(document));
        when(documentStorage.readFile("/tmp/id.pdf", "id.pdf", "application/pdf")).thenReturn(downloaded);

        LoanDocumentStorageService.DownloadedFile result =
                loanService.downloadDocument(1L, 9L, "conseiller@test.com");

        assertThat(result.fileName()).isEqualTo("id.pdf");
        assertThat(result.content()).containsExactly(1);
    }

    @Test
    void getAllApplications_returnsAdvisorVisibleLoans() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.SUBMITTED);

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findVisibleToAdvisor(eq(20L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        Page<LoanResponseDto> result = loanService.getAllApplications("conseiller@test.com", null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void proposeCounterOffer_throwsWhenOfferMatchesSystemOffer() {
        User advisor = advisorUser();
        LoanApplication loan = completeDraftLoan(clientUser());
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        loan.setAssignedAdvisor(advisor);
        ProposeOfferRequestDto request = new ProposeOfferRequestDto();
        request.setApprovedAmount(new BigDecimal("15000"));
        request.setApprovedDurationMonths(48);
        request.setInterestRate(new BigDecimal("3.85"));

        when(userRepository.findByEmail("conseiller@test.com")).thenReturn(Optional.of(advisor));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(requiredDocuments());
        doNothing().when(documentReviewService)
                .ensureRequiredDocumentsReadyForDecision(1L, "Contre-offre impossible");

        assertThatThrownBy(() -> loanService.proposeCounterOffer(1L, request, "conseiller@test.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("offre système");
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

    private static User adminUser() {
        return User.builder()
                .id(30L)
                .email("admin@test.com")
                .firstName("Alice")
                .lastName("Admin")
                .roles(Set.of(Role.builder().id(3L).name(ROLE_ADMIN).build()))
                .build();
    }
}
