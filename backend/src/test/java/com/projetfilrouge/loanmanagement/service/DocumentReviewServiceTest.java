package com.projetfilrouge.loanmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocument;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentReview;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentReviewStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentReviewRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentReviewResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentReviewServiceTest {

    @Mock
    private LoanDocumentReviewRepository reviewRepository;

    @Mock
    private LoanDocumentRepository documentRepository;

    @Mock
    private LoanApplicationEventRepository eventRepository;

    private DocumentReviewService documentReviewService;

    @BeforeEach
    void setUp() {
        documentReviewService = new DocumentReviewService(
                reviewRepository,
                documentRepository,
                eventRepository,
                new ObjectMapper()
        );
    }

    @Test
    void markValidated_createsValidatedReview() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(reviewRepository.findByLoanApplicationIdAndDocumentType(1L, LoanDocumentType.IDENTITY))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(LoanDocumentReview.class))).thenAnswer(invocation -> {
            LoanDocumentReview review = invocation.getArgument(0);
            review.setId(10L);
            return review;
        });

        documentReviewService.markValidated(loan, LoanDocumentType.IDENTITY);

        ArgumentCaptor<LoanDocumentReview> captor = ArgumentCaptor.forClass(LoanDocumentReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewStatus()).isEqualTo(LoanDocumentReviewStatus.VALIDATED);
        assertThat(captor.getValue().getDocumentType()).isEqualTo(LoanDocumentType.IDENTITY);
        assertThat(captor.getValue().getReviewComment()).isNull();
    }

    @Test
    void markRejected_storesTrimmedComment() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(reviewRepository.findByLoanApplicationIdAndDocumentType(1L, LoanDocumentType.PAYSLIPS))
                .thenReturn(Optional.of(existingReview(LoanDocumentType.PAYSLIPS, LoanDocumentReviewStatus.PENDING_REVIEW)));
        when(reviewRepository.save(any(LoanDocumentReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentReviewService.markRejected(loan, LoanDocumentType.PAYSLIPS, "  Illisible  ");

        ArgumentCaptor<LoanDocumentReview> captor = ArgumentCaptor.forClass(LoanDocumentReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewStatus()).isEqualTo(LoanDocumentReviewStatus.REJECTED);
        assertThat(captor.getValue().getReviewComment()).isEqualTo("Illisible");
    }

    @Test
    void markRejected_clearsBlankComment() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(reviewRepository.findByLoanApplicationIdAndDocumentType(1L, LoanDocumentType.TAX_NOTICE))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(LoanDocumentReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentReviewService.markRejected(loan, LoanDocumentType.TAX_NOTICE, "   ");

        ArgumentCaptor<LoanDocumentReview> captor = ArgumentCaptor.forClass(LoanDocumentReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewComment()).isNull();
    }

    @Test
    void initializePendingReviews_createsReviewsOnlyForUploadedTypes() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.SUBMITTED);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(
                        document(LoanDocumentType.IDENTITY),
                        document(LoanDocumentType.PAYSLIPS)
                ));
        when(reviewRepository.findByLoanApplicationIdAndDocumentType(any(), any()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(LoanDocumentReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentReviewService.initializePendingReviews(loan);

        ArgumentCaptor<LoanDocumentReview> captor = ArgumentCaptor.forClass(LoanDocumentReview.class);
        verify(reviewRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(LoanDocumentReview::getDocumentType)
                .containsExactlyInAnyOrder(LoanDocumentType.IDENTITY, LoanDocumentType.PAYSLIPS);
        assertThat(captor.getAllValues())
                .allMatch(review -> review.getReviewStatus() == LoanDocumentReviewStatus.PENDING_REVIEW);
    }

    @Test
    void getDocumentReviews_returnsPendingStatusForUploadedDocuments() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(allRequiredDocuments());
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of());
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of());

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews).hasSize(5);
        assertThat(reviews)
                .extracting(LoanDocumentReviewResponseDto::getStatus)
                .containsOnly("pending_review");
    }

    @Test
    void getDocumentReviews_returnsValidatedStatusFromStoredReview() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(allRequiredDocuments());
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of(
                        existingReview(LoanDocumentType.IDENTITY, LoanDocumentReviewStatus.VALIDATED)
                ));

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews).hasSize(5);
        assertThat(reviews.stream().filter(r -> r.getDocumentType() == LoanDocumentType.IDENTITY).findFirst())
                .get()
                .satisfies(identity -> {
                    assertThat(identity.getStatus()).isEqualTo("validated");
                });
        assertThat(reviews.stream().filter(r -> r.getDocumentType() != LoanDocumentType.IDENTITY))
                .allMatch(review -> "pending_review".equals(review.getStatus()));
    }

    @Test
    void ensureAllRequiredDocumentsValidated_throwsWhenDocumentNotValidated() {
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of(
                        existingReview(LoanDocumentType.IDENTITY, LoanDocumentReviewStatus.VALIDATED),
                        existingReview(LoanDocumentType.PAYSLIPS, LoanDocumentReviewStatus.PENDING_REVIEW)
                ));

        assertThatThrownBy(() -> documentReviewService.ensureAllRequiredDocumentsValidated(1L, "Approbation impossible"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Approbation impossible : toutes les pièces obligatoires doivent être validées.");
    }

    @Test
    void ensureRequiredDocumentsReadyForDecision_throwsWhenDocumentRejected() {
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of(
                        existingReview(LoanDocumentType.IDENTITY, LoanDocumentReviewStatus.REJECTED)
                ));

        assertThatThrownBy(() -> documentReviewService.ensureRequiredDocumentsReadyForDecision(1L, "Contre-offre impossible"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Contre-offre impossible : des pièces obligatoires sont en attente de correction.");
    }

    @Test
    void ensureRequiredDocumentsReadyForDecision_passesWhenAllRequiredDocumentsValidated() {
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(allValidatedReviews());

        assertThatCode(
                () -> documentReviewService.ensureRequiredDocumentsReadyForDecision(1L, "Approbation impossible")
        ).doesNotThrowAnyException();
    }

    @Test
    void markPendingReview_upsertsPendingStatus() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(reviewRepository.findByLoanApplicationIdAndDocumentType(1L, LoanDocumentType.OTHER))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(LoanDocumentReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentReviewService.markPendingReview(loan, LoanDocumentType.OTHER);

        ArgumentCaptor<LoanDocumentReview> captor = ArgumentCaptor.forClass(LoanDocumentReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewStatus()).isEqualTo(LoanDocumentReviewStatus.PENDING_REVIEW);
    }

    @Test
    void getDocumentReviews_derivesRejectedStatusFromHistory() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(document(LoanDocumentType.IDENTITY)));
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of());
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(historyEvent(
                        LoanApplicationEventType.DOCUMENT_REJECTED,
                        "{\"documentType\":\"IDENTITY\",\"comment\":\"Illisible\"}"
                )));

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews.stream().filter(r -> r.getDocumentType() == LoanDocumentType.IDENTITY).findFirst())
                .get()
                .satisfies(identity -> {
                    assertThat(identity.getStatus()).isEqualTo("rejected");
                    assertThat(identity.getComment()).isEqualTo("Illisible");
                });
    }

    @Test
    void getDocumentReviews_showsMissingUploadForRequiredDocuments() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.DRAFT);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of());
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of());
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of());

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews)
                .hasSize(5)
                .allMatch(review -> "missing_upload".equals(review.getStatus()));
    }

    @Test
    void getDocumentReviews_includesOtherDocumentWhenUploaded() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(List.of(document(LoanDocumentType.OTHER)));
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of());
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of());

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews).hasSize(6);
        assertThat(reviews.stream().filter(r -> r.getDocumentType() == LoanDocumentType.OTHER).findFirst())
                .get()
                .extracting(LoanDocumentReviewResponseDto::getStatus)
                .isEqualTo("pending_review");
    }

    @Test
    void getDocumentReviews_returnsRejectedStatusFromStoredReview() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.UNDER_REVIEW);
        LoanDocumentReview rejected = existingReview(LoanDocumentType.PAYSLIPS, LoanDocumentReviewStatus.REJECTED);
        rejected.setReviewComment("Document expiré");

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(allRequiredDocuments());
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of(rejected));

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews.stream().filter(r -> r.getDocumentType() == LoanDocumentType.PAYSLIPS).findFirst())
                .get()
                .satisfies(payslips -> {
                    assertThat(payslips.getStatus()).isEqualTo("rejected");
                    assertThat(payslips.getComment()).isEqualTo("Document expiré");
                });
    }

    @Test
    void getDocumentReviews_validatesByDefaultOnApprovedLoan() {
        LoanApplication loan = loan(1L, LoanApplicationStatus.APPROVED);

        when(documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(1L))
                .thenReturn(allRequiredDocuments());
        when(reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(1L))
                .thenReturn(List.of());
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(historyEvent(
                        LoanApplicationEventType.DOCUMENT_VALIDATED,
                        "{\"documentType\":\"IDENTITY\"}"
                )));

        List<LoanDocumentReviewResponseDto> reviews = documentReviewService.getDocumentReviews(loan);

        assertThat(reviews.stream().filter(r -> r.getDocumentType() == LoanDocumentType.IDENTITY).findFirst())
                .get()
                .extracting(LoanDocumentReviewResponseDto::getStatus)
                .isEqualTo("validated");
    }

    private static LoanApplicationEvent historyEvent(LoanApplicationEventType type, String payloadJson) {
        return LoanApplicationEvent.builder()
                .eventType(type)
                .payloadJson(payloadJson)
                .build();
    }

    private static LoanApplication loan(Long id, LoanApplicationStatus status) {
        return LoanApplication.builder()
                .id(id)
                .reference("LOAN-TEST")
                .status(status)
                .build();
    }

    private static LoanDocument document(LoanDocumentType type) {
        return LoanDocument.builder().documentType(type).build();
    }

    private static List<LoanDocument> allRequiredDocuments() {
        return EnumSet.allOf(LoanDocumentType.class).stream()
                .filter(type -> type != LoanDocumentType.OTHER)
                .map(DocumentReviewServiceTest::document)
                .toList();
    }

    private static LoanDocumentReview existingReview(LoanDocumentType type, LoanDocumentReviewStatus status) {
        return LoanDocumentReview.builder()
                .documentType(type)
                .reviewStatus(status)
                .build();
    }

    private static List<LoanDocumentReview> allValidatedReviews() {
        Set<LoanDocumentType> required = EnumSet.of(
                LoanDocumentType.IDENTITY,
                LoanDocumentType.PAYSLIPS,
                LoanDocumentType.TAX_NOTICE,
                LoanDocumentType.BANK_STATEMENTS,
                LoanDocumentType.PROOF_OF_ADDRESS
        );
        return required.stream()
                .map(type -> existingReview(type, LoanDocumentReviewStatus.VALIDATED))
                .toList();
    }
}
