package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DocumentValidationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanDocument;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentReview;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentReviewStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentReviewRepository;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifGroupResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifItemResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrégation des justificatifs client (onglet « Mes justificatifs »). US-6.1
 *
 * <p>Réutilise {@link LoanDocument} (pièces déposées au wizard) et croise le statut
 * d'instruction conseiller ({@link LoanDocumentReview}) pour afficher En attente /
 * Validé / Refusé. Ne duplique aucune entité ni aucun stockage.</p>
 */
@Service
@RequiredArgsConstructor
public class ClientDocumentsService {

    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanDocumentReviewRepository loanDocumentReviewRepository;
    private final LoanDocumentStorageService loanDocumentStorageService;

    @Transactional(readOnly = true)
    public List<JustificatifGroupResponseDto> getJustificatifsGroupedByApplication(String clientEmail) {
        List<LoanDocument> documents = loanDocumentRepository.findAllByApplicantEmail(clientEmail);

        // La requête est déjà triée par dossier (id desc) puis date — on préserve cet ordre.
        Map<Long, JustificatifGroupResponseDto> groups = new LinkedHashMap<>();

        for (LoanDocument document : documents) {
            LoanApplication application = document.getLoanApplication();
            Long applicationId = application.getId();

            JustificatifGroupResponseDto group = groups.computeIfAbsent(applicationId, id ->
                    JustificatifGroupResponseDto.builder()
                            .loanApplicationId(applicationId)
                            .loanReference(application.getReference())
                            .loanStatus(application.getStatus().name())
                            .submittedAt(application.getSubmittedAt())
                            .updatedAt(application.getUpdatedAt())
                            .documents(new ArrayList<>())
                            .build());

            group.getDocuments().add(toItem(application, document));
        }

        return new ArrayList<>(groups.values());
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadJustificatif(String clientEmail, Long documentId) {
        LoanDocument document = loanDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif introuvable"));

        String ownerEmail = document.getLoanApplication().getApplicant().getEmail();
        if (!ownerEmail.equalsIgnoreCase(clientEmail)) {
            throw new ForbiddenOperationException("Accès refusé à ce justificatif.");
        }

        LoanDocumentStorageService.DownloadedFile file = loanDocumentStorageService.readFile(
                document.getStoragePath(),
                document.getOriginalFileName(),
                document.getContentType()
        );

        String contentType = file.contentType() != null ? file.contentType() : "application/octet-stream";
        return new DocumentDownload(file.fileName(), contentType, file.content());
    }

    private JustificatifItemResponseDto toItem(LoanApplication application, LoanDocument document) {
        DocumentReviewInfo reviewInfo = resolveReview(application.getId(), document.getDocumentType());

        return JustificatifItemResponseDto.builder()
                .documentId(document.getId())
                .documentType(document.getDocumentType())
                .documentTypeLabel(label(document))
                .fileName(document.getOriginalFileName())
                .uploadedAt(document.getUploadedAt())
                .validationStatus(reviewInfo.status())
                .rejectionReason(reviewInfo.rejectionReason())
                .downloadable(true)
                .build();
    }

    private DocumentReviewInfo resolveReview(Long applicationId, LoanDocumentType documentType) {
        return loanDocumentReviewRepository
                .findByLoanApplicationIdAndDocumentType(applicationId, documentType)
                .map(this::mapReview)
                .orElse(new DocumentReviewInfo(DocumentValidationStatus.PENDING, null));
    }

    private DocumentReviewInfo mapReview(LoanDocumentReview review) {
        DocumentValidationStatus status = switch (review.getReviewStatus()) {
            case VALIDATED -> DocumentValidationStatus.VALIDATED;
            case REJECTED -> DocumentValidationStatus.REJECTED;
            case PENDING_REVIEW -> DocumentValidationStatus.PENDING;
        };
        String reason = review.getReviewStatus() == LoanDocumentReviewStatus.REJECTED
                ? review.getReviewComment()
                : null;
        return new DocumentReviewInfo(status, reason);
    }

    private String label(LoanDocument document) {
        if (document.getDocumentType() == LoanDocumentType.OTHER
                && document.getDisplayName() != null
                && !document.getDisplayName().isBlank()) {
            return document.getDisplayName();
        }
        return frenchLabel(document.getDocumentType());
    }

    private String frenchLabel(LoanDocumentType type) {
        return switch (type) {
            case IDENTITY -> "Pièce d'identité";
            case PAYSLIPS -> "Bulletins de salaire";
            case TAX_NOTICE -> "Avis d'imposition";
            case BANK_STATEMENTS -> "Relevés bancaires";
            case PROOF_OF_ADDRESS -> "Justificatif de domicile";
            case OTHER -> "Autre document";
        };
    }

    private record DocumentReviewInfo(DocumentValidationStatus status, String rejectionReason) {
    }
}
