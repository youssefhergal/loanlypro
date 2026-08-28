package com.projetfilrouge.loanmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentReviewRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentReviewResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentReviewService {

    private static final Set<LoanDocumentType> REQUIRED_DOCUMENT_TYPES = EnumSet.of(
            LoanDocumentType.IDENTITY,
            LoanDocumentType.PAYSLIPS,
            LoanDocumentType.TAX_NOTICE,
            LoanDocumentType.BANK_STATEMENTS,
            LoanDocumentType.PROOF_OF_ADDRESS
    );

    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_VALIDATED = "validated";
    private static final String STATUS_PENDING_REVIEW = "pending_review";
    private static final String STATUS_MISSING_UPLOAD = "missing_upload";

    private static final Set<LoanDocumentType> ALL_REVIEWABLE_TYPES = EnumSet.of(
            LoanDocumentType.IDENTITY,
            LoanDocumentType.PAYSLIPS,
            LoanDocumentType.TAX_NOTICE,
            LoanDocumentType.BANK_STATEMENTS,
            LoanDocumentType.PROOF_OF_ADDRESS,
            LoanDocumentType.OTHER
    );

    private final LoanDocumentReviewRepository reviewRepository;
    private final LoanDocumentRepository documentRepository;
    private final LoanApplicationEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<LoanDocumentReviewResponseDto> getDocumentReviews(LoanApplication loan) {
        Set<LoanDocumentType> presentTypes = presentDocumentTypes(loan.getId());
        Map<LoanDocumentType, EffectiveReview> reviews = effectiveReviews(loan.getId());

        List<LoanDocumentReviewResponseDto> result = new ArrayList<>();
        for (LoanDocumentType type : ALL_REVIEWABLE_TYPES) {
            boolean required = REQUIRED_DOCUMENT_TYPES.contains(type);
            boolean hasFile = presentTypes.contains(type);
            if (!required && !hasFile) {
                continue;
            }
            result.add(toDto(type, reviews.get(type), hasFile, loan.getStatus()));
        }
        return result;
    }

    @Transactional
    public void initializePendingReviews(LoanApplication loan) {
        Set<LoanDocumentType> presentTypes = presentDocumentTypes(loan.getId());
        for (LoanDocumentType required : REQUIRED_DOCUMENT_TYPES) {
            if (!presentTypes.contains(required)) {
                continue;
            }
            upsertReview(loan, required, LoanDocumentReviewStatus.PENDING_REVIEW, null);
        }
    }

    @Transactional
    public void markPendingReview(LoanApplication loan, LoanDocumentType documentType) {
        upsertReview(loan, documentType, LoanDocumentReviewStatus.PENDING_REVIEW, null);
    }

    @Transactional
    public void markValidated(LoanApplication loan, LoanDocumentType documentType) {
        upsertReview(loan, documentType, LoanDocumentReviewStatus.VALIDATED, null);
    }

    @Transactional
    public void markRejected(LoanApplication loan, LoanDocumentType documentType, String comment) {
        String trimmed = comment != null ? comment.trim() : null;
        upsertReview(
                loan,
                documentType,
                LoanDocumentReviewStatus.REJECTED,
                trimmed != null && !trimmed.isBlank() ? trimmed : null
        );
    }

    @Transactional(readOnly = true)
    public void ensureRequiredDocumentsReadyForDecision(Long loanId, String actionPrefix) {
        ensureNoRejectedRequiredDocuments(loanId, actionPrefix);
        ensureAllRequiredDocumentsValidated(loanId, actionPrefix);
    }

    @Transactional(readOnly = true)
    public void ensureAllRequiredDocumentsValidated(Long loanId, String actionPrefix) {
        Map<LoanDocumentType, EffectiveReview> reviews = effectiveReviews(loanId);
        for (LoanDocumentType required : REQUIRED_DOCUMENT_TYPES) {
            EffectiveReview review = reviews.get(required);
            if (review == null || review.status() != LoanDocumentReviewStatus.VALIDATED) {
                throw new BusinessRuleException(
                        actionPrefix + " : toutes les pièces obligatoires doivent être validées."
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public void ensureNoRejectedRequiredDocuments(Long loanId, String actionPrefix) {
        Map<LoanDocumentType, EffectiveReview> reviews = effectiveReviews(loanId);
        for (LoanDocumentType required : REQUIRED_DOCUMENT_TYPES) {
            EffectiveReview review = reviews.get(required);
            if (review != null && review.status() == LoanDocumentReviewStatus.REJECTED) {
                throw new BusinessRuleException(
                        actionPrefix + " : des pièces obligatoires sont en attente de correction."
                );
            }
        }
    }

    private Map<LoanDocumentType, EffectiveReview> effectiveReviews(Long loanId) {
        Map<LoanDocumentType, LoanDocumentReview> stored = indexReviews(loanId);
        if (!stored.isEmpty()) {
            Map<LoanDocumentType, EffectiveReview> result = new EnumMap<>(LoanDocumentType.class);
            stored.forEach((type, review) -> result.put(
                    type,
                    new EffectiveReview(
                            review.getReviewStatus(),
                            review.getReviewComment(),
                            review.getUpdatedAt()
                    )
            ));
            return result;
        }

        Map<LoanDocumentType, HistoryReviewState> fromHistory = documentReviewStatusesFromHistory(loanId);
        Set<LoanDocumentType> presentTypes = presentDocumentTypes(loanId);
        Map<LoanDocumentType, EffectiveReview> result = new EnumMap<>(LoanDocumentType.class);
        for (LoanDocumentType type : ALL_REVIEWABLE_TYPES) {
            boolean required = REQUIRED_DOCUMENT_TYPES.contains(type);
            boolean hasFile = presentTypes.contains(type);
            if (!required && !hasFile) {
                continue;
            }
            HistoryReviewState state = fromHistory.get(type);
            if (state != null) {
                LoanDocumentReviewStatus status = mapHistoryStatus(state.status());
                if (status != null) {
                    result.put(type, new EffectiveReview(status, state.comment(), null));
                }
            } else {
                result.put(type, new EffectiveReview(LoanDocumentReviewStatus.PENDING_REVIEW, null, null));
            }
        }
        return result;
    }

    private Map<LoanDocumentType, HistoryReviewState> documentReviewStatusesFromHistory(Long loanId) {
        List<LoanApplicationEvent> events = eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(loanId);
        Map<LoanDocumentType, HistoryReviewState> statuses = new EnumMap<>(LoanDocumentType.class);
        for (LoanApplicationEvent event : events) {
            Map<String, Object> payload = deserializePayload(event.getPayloadJson());
            LoanDocumentType type = documentTypeFromPayload(event.getEventType(), payload);
            if (type == null) {
                continue;
            }
            switch (event.getEventType()) {
                case DOCUMENT_REJECTED -> statuses.put(
                        type,
                        new HistoryReviewState(STATUS_REJECTED, stringVal(payload.get("comment")))
                );
                case DOCUMENT_VALIDATED -> statuses.put(type, new HistoryReviewState(STATUS_VALIDATED, null));
                case DOCUMENT_UPLOADED -> {
                    if (Boolean.TRUE.equals(payload.get("complement"))) {
                        statuses.put(type, new HistoryReviewState(STATUS_PENDING_REVIEW, null));
                    }
                }
                default -> {
                    // no-op
                }
            }
        }
        return statuses;
    }

    private LoanDocumentReviewStatus mapHistoryStatus(String status) {
        return switch (status) {
            case STATUS_VALIDATED -> LoanDocumentReviewStatus.VALIDATED;
            case STATUS_REJECTED -> LoanDocumentReviewStatus.REJECTED;
            case STATUS_PENDING_REVIEW -> LoanDocumentReviewStatus.PENDING_REVIEW;
            default -> null;
        };
    }

    private LoanDocumentType documentTypeFromPayload(
            LoanApplicationEventType type,
            Map<String, Object> payload
    ) {
        if (type != LoanApplicationEventType.DOCUMENT_REJECTED
                && type != LoanApplicationEventType.DOCUMENT_VALIDATED
                && type != LoanApplicationEventType.DOCUMENT_UPLOADED) {
            return null;
        }
        Object raw = payload.get("documentType");
        if (raw == null) {
            return null;
        }
        try {
            return LoanDocumentType.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, Object> deserializePayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String stringVal(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private void upsertReview(
            LoanApplication loan,
            LoanDocumentType documentType,
            LoanDocumentReviewStatus status,
            String comment
    ) {
        LoanDocumentReview review = reviewRepository
                .findByLoanApplicationIdAndDocumentType(loan.getId(), documentType)
                .orElseGet(() -> LoanDocumentReview.builder()
                        .loanApplication(loan)
                        .documentType(documentType)
                        .build());
        review.setReviewStatus(status);
        review.setReviewComment(comment);
        reviewRepository.save(review);
    }

    private Map<LoanDocumentType, LoanDocumentReview> indexReviews(Long loanId) {
        Map<LoanDocumentType, LoanDocumentReview> map = new EnumMap<>(LoanDocumentType.class);
        for (LoanDocumentReview review : reviewRepository.findByLoanApplicationIdOrderByDocumentTypeAsc(loanId)) {
            map.put(review.getDocumentType(), review);
        }
        return map;
    }

    private Set<LoanDocumentType> presentDocumentTypes(Long loanId) {
        Set<LoanDocumentType> present = EnumSet.noneOf(LoanDocumentType.class);
        documentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loanId).stream()
                .map(LoanDocument::getDocumentType)
                .forEach(present::add);
        return present;
    }

    private LoanDocumentReviewResponseDto toDto(
            LoanDocumentType type,
            EffectiveReview review,
            boolean hasFile,
            LoanApplicationStatus loanStatus
    ) {
        if (!hasFile && REQUIRED_DOCUMENT_TYPES.contains(type)) {
            return LoanDocumentReviewResponseDto.builder()
                    .documentType(type)
                    .status(STATUS_MISSING_UPLOAD)
                    .comment("Document obligatoire non fourni.")
                    .build();
        }
        if (review == null) {
            return LoanDocumentReviewResponseDto.builder()
                    .documentType(type)
                    .status(defaultStatusForLoan(hasFile, loanStatus))
                    .build();
        }
        return LoanDocumentReviewResponseDto.builder()
                .documentType(type)
                .status(mapReviewStatus(review.status(), hasFile, loanStatus))
                .comment(review.comment())
                .updatedAt(review.updatedAt())
                .build();
    }

    private String defaultStatusForLoan(boolean hasFile, LoanApplicationStatus loanStatus) {
        if (!hasFile) {
            return STATUS_MISSING_UPLOAD;
        }
        if (loanStatus == LoanApplicationStatus.APPROVED || loanStatus == LoanApplicationStatus.REJECTED) {
            return STATUS_VALIDATED;
        }
        return STATUS_PENDING_REVIEW;
    }

    private String mapReviewStatus(
            LoanDocumentReviewStatus status,
            boolean hasFile,
            LoanApplicationStatus loanStatus
    ) {
        if (!hasFile) {
            return STATUS_MISSING_UPLOAD;
        }
        return switch (status) {
            case VALIDATED -> STATUS_VALIDATED;
            case REJECTED -> STATUS_REJECTED;
            case PENDING_REVIEW -> defaultStatusForLoan(true, loanStatus);
        };
    }

    private record HistoryReviewState(String status, String comment) {}

    private record EffectiveReview(
            LoanDocumentReviewStatus status,
            String comment,
            java.time.Instant updatedAt
    ) {}
}
