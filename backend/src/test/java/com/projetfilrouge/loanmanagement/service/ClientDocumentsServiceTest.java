package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DocumentValidationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocument;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentReview;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentReviewStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentReviewRepository;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifGroupResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientDocumentsServiceTest {

    private static final String CLIENT_EMAIL = "client@test.com";

    @Mock
    private LoanDocumentRepository loanDocumentRepository;

    @Mock
    private LoanDocumentReviewRepository loanDocumentReviewRepository;

    @Mock
    private LoanDocumentStorageService loanDocumentStorageService;

    @InjectMocks
    private ClientDocumentsService service;

    @Test
    void getJustificatifs_groupsByApplicationAndMapsReviewStatus() {
        LoanApplication application = application(1L, "LF-DEMO-0001", LoanApplicationStatus.APPROVED, CLIENT_EMAIL);
        LoanDocument identity = document(10L, application, LoanDocumentType.IDENTITY, "carte.pdf");
        LoanDocument payslips = document(11L, application, LoanDocumentType.PAYSLIPS, "salaire.pdf");

        when(loanDocumentRepository.findAllByApplicantEmail(CLIENT_EMAIL))
                .thenReturn(List.of(identity, payslips));
        when(loanDocumentReviewRepository.findByLoanApplicationIdAndDocumentType(1L, LoanDocumentType.IDENTITY))
                .thenReturn(Optional.of(review(LoanDocumentReviewStatus.VALIDATED, null)));
        when(loanDocumentReviewRepository.findByLoanApplicationIdAndDocumentType(1L, LoanDocumentType.PAYSLIPS))
                .thenReturn(Optional.empty());

        List<JustificatifGroupResponseDto> groups = service.getJustificatifsGroupedByApplication(CLIENT_EMAIL);

        assertThat(groups).hasSize(1);
        JustificatifGroupResponseDto group = groups.get(0);
        assertThat(group.getLoanReference()).isEqualTo("LF-DEMO-0001");
        assertThat(group.getDocuments()).hasSize(2);

        assertThat(group.getDocuments().get(0).getDocumentTypeLabel()).isEqualTo("Pièce d'identité");
        assertThat(group.getDocuments().get(0).getValidationStatus()).isEqualTo(DocumentValidationStatus.VALIDATED);
        assertThat(group.getDocuments().get(1).getValidationStatus()).isEqualTo(DocumentValidationStatus.PENDING);
    }

    @Test
    void downloadJustificatif_returnsFileForOwner() {
        LoanApplication application = application(1L, "LF-DEMO-0001", LoanApplicationStatus.APPROVED, CLIENT_EMAIL);
        LoanDocument identity = document(10L, application, LoanDocumentType.IDENTITY, "carte.pdf");

        when(loanDocumentRepository.findById(10L)).thenReturn(Optional.of(identity));
        when(loanDocumentStorageService.readFile(any(), eq("carte.pdf"), eq("application/pdf")))
                .thenReturn(new LoanDocumentStorageService.DownloadedFile(
                        "carte.pdf", "application/pdf", new byte[]{1, 2, 3}));

        DocumentDownload download = service.downloadJustificatif(CLIENT_EMAIL, 10L);

        assertThat(download.fileName()).isEqualTo("carte.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(download.content()).hasSize(3);
    }

    @Test
    void downloadJustificatif_forbiddenForNonOwner() {
        LoanApplication application = application(1L, "LF-DEMO-0001", LoanApplicationStatus.APPROVED, "autre@test.com");
        LoanDocument identity = document(10L, application, LoanDocumentType.IDENTITY, "carte.pdf");
        when(loanDocumentRepository.findById(10L)).thenReturn(Optional.of(identity));

        assertThatThrownBy(() -> service.downloadJustificatif(CLIENT_EMAIL, 10L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void downloadJustificatif_notFound() {
        when(loanDocumentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadJustificatif(CLIENT_EMAIL, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ----------------------------------------------------------------- fixtures */

    private LoanApplication application(Long id, String reference, LoanApplicationStatus status, String email) {
        return LoanApplication.builder()
                .id(id)
                .reference(reference)
                .status(status)
                .applicant(User.builder().email(email).firstName("Jean").lastName("Dupont").build())
                .build();
    }

    private LoanDocument document(Long id, LoanApplication application, LoanDocumentType type, String fileName) {
        return LoanDocument.builder()
                .id(id)
                .loanApplication(application)
                .documentType(type)
                .originalFileName(fileName)
                .storedFileName("stored-" + fileName)
                .storagePath("loan-1/stored-" + fileName)
                .contentType("application/pdf")
                .fileSizeBytes(1024L)
                .uploadedAt(Instant.now())
                .build();
    }

    private LoanDocumentReview review(LoanDocumentReviewStatus status, String comment) {
        return LoanDocumentReview.builder()
                .reviewStatus(status)
                .reviewComment(comment)
                .build();
    }
}
