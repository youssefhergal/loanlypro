package com.projetfilrouge.loanmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoanApplicationHistoryService {

    private static final Map<LoanDocumentType, String> DOCUMENT_LABELS = Map.of(
            LoanDocumentType.IDENTITY, "Pièce d'identité",
            LoanDocumentType.PAYSLIPS, "Bulletins de salaire",
            LoanDocumentType.TAX_NOTICE, "Avis d'imposition",
            LoanDocumentType.BANK_STATEMENTS, "Relevés bancaires",
            LoanDocumentType.PROOF_OF_ADDRESS, "Justificatif de domicile",
            LoanDocumentType.OTHER, "Autre document"
    );

    private final LoanApplicationEventRepository eventRepository;
    private final LoanApplicationRepository loanRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LoanApplicationEvent recordEvent(
            LoanApplication loan,
            LoanApplicationEventType type,
            LoanEventActorType actorType,
            String actorEmail,
            String actorDisplayName,
            Map<String, Object> payload
    ) {
        return recordEvent(loan, type, actorType, actorEmail, actorDisplayName, payload, Instant.now());
    }

    @Transactional
    public LoanApplicationEvent recordEvent(
            LoanApplication loan,
            LoanApplicationEventType type,
            LoanEventActorType actorType,
            String actorEmail,
            String actorDisplayName,
            Map<String, Object> payload,
            Instant occurredAt
    ) {
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .loanApplication(loan)
                .eventType(type)
                .occurredAt(occurredAt)
                .actorType(actorType)
                .actorEmail(actorEmail)
                .actorDisplayName(actorDisplayName)
                .payloadJson(serializePayload(payload))
                .build();
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public LoanHistoryEventResponseDto mapEventToDisplayDto(LoanApplicationEvent event) {
        LoanApplication loan = event.getLoanApplication();
        List<LoanApplicationEvent> events = eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(loan.getId());
        int index = 0;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                index = i;
                break;
            }
        }
        return toDisplayDto(event, loan, index, events.size());
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getHistory(Long loanId) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        List<LoanApplicationEvent> events = eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(loanId);
        List<LoanHistoryEventResponseDto> result = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            result.add(toDisplayDto(events.get(i), loan, i, events.size()));
        }
        result = consolidateDocumentUploadEvents(result);
        appendProjectedEvents(loan, result);
        return result;
    }

    /**
     * Regroupe les anciens événements « un fichier = une ligne » (hors compléments) en une seule entrée.
     */
    private List<LoanHistoryEventResponseDto> consolidateDocumentUploadEvents(
            List<LoanHistoryEventResponseDto> events
    ) {
        List<LoanHistoryEventResponseDto> consolidated = new ArrayList<>();
        List<LoanHistoryEventResponseDto> uploadBatch = new ArrayList<>();

        for (LoanHistoryEventResponseDto event : events) {
            if (isInitialDocumentUpload(event)) {
                uploadBatch.add(event);
                continue;
            }
            if (!uploadBatch.isEmpty()) {
                consolidated.add(mergeDocumentUploadBatch(uploadBatch));
                uploadBatch = new ArrayList<>();
            }
            consolidated.add(event);
        }
        if (!uploadBatch.isEmpty()) {
            consolidated.add(mergeDocumentUploadBatch(uploadBatch));
        }
        return consolidated;
    }

    private boolean isInitialDocumentUpload(LoanHistoryEventResponseDto event) {
        if (event.getEventType() != LoanApplicationEventType.DOCUMENT_UPLOADED) {
            return false;
        }
        if (Boolean.TRUE.equals(event.getComplement())) {
            return false;
        }
        return event.getId() != null;
    }

    private LoanHistoryEventResponseDto mergeDocumentUploadBatch(List<LoanHistoryEventResponseDto> batch) {
        LoanHistoryEventResponseDto first = batch.get(0);
        if (batch.size() == 1) {
            return first;
        }
        int count = batch.size();
        return LoanHistoryEventResponseDto.builder()
                .id(first.getId())
                .eventType(LoanApplicationEventType.DOCUMENT_UPLOADED)
                .occurredAt(first.getOccurredAt())
                .actorType(first.getActorType())
                .actorDisplayName(first.getActorDisplayName())
                .title("Pièces justificatives déposées")
                .description(bundledDocumentsDescription(count, REQUIRED_DOCUMENT_TYPES.size()))
                .state(first.getState())
                .build();
    }

    private static final Set<LoanDocumentType> REQUIRED_DOCUMENT_TYPES = EnumSet.of(
            LoanDocumentType.IDENTITY,
            LoanDocumentType.PAYSLIPS,
            LoanDocumentType.TAX_NOTICE,
            LoanDocumentType.BANK_STATEMENTS,
            LoanDocumentType.PROOF_OF_ADDRESS
    );

    private void appendProjectedEvents(LoanApplication loan, List<LoanHistoryEventResponseDto> result) {
        if (loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW) {
            result.add(LoanHistoryEventResponseDto.builder()
                    .title("Décision du comité")
                    .description("Le comité de crédit rendra sa décision après analyse complète du dossier.")
                    .state("upcoming")
                    .build());
            result.add(LoanHistoryEventResponseDto.builder()
                    .title("Déblocage des fonds")
                    .description("En cas d'approbation, les fonds seront disponibles sous 24 à 48 heures.")
                    .state("upcoming")
                    .build());
        } else if (loan.getStatus() == LoanApplicationStatus.APPROVED) {
            boolean hasFunds = result.stream()
                    .anyMatch(e -> e.getEventType() == LoanApplicationEventType.FUNDS_RELEASED);
            if (!hasFunds) {
                result.add(LoanHistoryEventResponseDto.builder()
                        .title("Déblocage des fonds")
                        .description("Les fonds seront disponibles sous 24 à 48 heures.")
                        .state("current")
                        .build());
            }
        } else if (loan.getStatus() == LoanApplicationStatus.SUBMITTED) {
            result.add(LoanHistoryEventResponseDto.builder()
                    .title("Analyse du dossier")
                    .description("Un conseiller va examiner votre dossier sous 2 à 5 jours ouvrés.")
                    .state("upcoming")
                    .build());
        }
    }

    private LoanHistoryEventResponseDto toDisplayDto(
            LoanApplicationEvent event,
            LoanApplication loan,
            int index,
            int total
    ) {
        Map<String, Object> payload = deserializePayload(event.getPayloadJson());
        String title = resolveTitle(event, payload, loan);
        String description = resolveDescription(event, payload, loan);
        String state = resolveState(event, loan, index, total);

        LoanDocumentType documentType = documentTypeFromPayload(event.getEventType(), payload);
        String comment = commentFromPayload(event.getEventType(), payload);
        Boolean complement = complementFromPayload(event.getEventType(), payload);

        return LoanHistoryEventResponseDto.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .occurredAt(event.getOccurredAt())
                .actorType(event.getActorType())
                .actorDisplayName(event.getActorDisplayName())
                .title(title)
                .description(description)
                .state(state)
                .documentType(documentType)
                .comment(comment)
                .complement(complement)
                .build();
    }

    private LoanDocumentType documentTypeFromPayload(
            LoanApplicationEventType type,
            Map<String, Object> payload
    ) {
        if (type != LoanApplicationEventType.DOCUMENT_UPLOADED
                && type != LoanApplicationEventType.DOCUMENT_REJECTED
                && type != LoanApplicationEventType.DOCUMENT_VALIDATED) {
            return null;
        }
        Object raw = payload.get("documentType");
        if (raw == null) {
            return null;
        }
        try {
            return LoanDocumentType.valueOf(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String commentFromPayload(LoanApplicationEventType type, Map<String, Object> payload) {
        if (type == LoanApplicationEventType.DOCUMENT_REJECTED) {
            return stringVal(payload.get("comment"));
        }
        return null;
    }

    private Boolean complementFromPayload(LoanApplicationEventType type, Map<String, Object> payload) {
        if (type == LoanApplicationEventType.DOCUMENT_UPLOADED) {
            return Boolean.TRUE.equals(payload.get("complement"));
        }
        return null;
    }

    private String resolveTitle(LoanApplicationEvent event, Map<String, Object> payload, LoanApplication loan) {
        return switch (event.getEventType()) {
            case APPLICATION_CREATED -> "Brouillon créé";
            case APPLICATION_SUBMITTED -> "Demande soumise";
            case DOCUMENT_UPLOADED -> Boolean.TRUE.equals(payload.get("bundled"))
                    ? "Pièces justificatives déposées"
                    : "Document déposé";
            case ADVISOR_ASSIGNED -> "Conseiller affecté";
            case REVIEW_STARTED -> "Analyse financière";
            case DOCUMENT_REJECTED -> "Complément demandé";
            case DOCUMENT_VALIDATED -> "Document validé";
            case APPLICATION_APPROVED -> "Demande approuvée";
            case APPLICATION_REJECTED -> "Demande refusée";
            case APPLICATION_CANCELLED -> "Demande annulée";
            case FUNDS_RELEASED -> "Fonds débloqués";
        };
    }

    private String resolveDescription(LoanApplicationEvent event, Map<String, Object> payload, LoanApplication loan) {
        return switch (event.getEventType()) {
            case APPLICATION_CREATED -> "Votre demande a été enregistrée. Référence : #" + loan.getReference() + ".";
            case APPLICATION_SUBMITTED ->
                    "Votre dossier a été transmis pour étude. Référence : #" + loan.getReference() + ".";
            case DOCUMENT_UPLOADED -> {
                if (Boolean.TRUE.equals(payload.get("bundled"))) {
                    int count = intVal(payload.get("documentCount"), REQUIRED_DOCUMENT_TYPES.size());
                    yield bundledDocumentsDescription(count, REQUIRED_DOCUMENT_TYPES.size());
                }
                String label = documentLabel(payload);
                String file = stringVal(payload.get("fileName"));
                boolean complement = Boolean.TRUE.equals(payload.get("complement"));
                String prefix = complement ? "Nouveau dépôt — " : "";
                yield file != null ? prefix + label + " — " + file : prefix + label + " reçu.";
            }
            case ADVISOR_ASSIGNED -> {
                String name = event.getActorDisplayName();
                if (name == null || name.isBlank()) {
                    name = stringVal(payload.get("advisorName"));
                }
                yield name != null && !name.isBlank()
                        ? name + " est désormais en charge de votre dossier."
                        : "Un conseiller a été affecté à votre dossier.";
            }
            case REVIEW_STARTED ->
                    "Votre dossier est en cours d'analyse. Durée estimée : 2 à 5 jours ouvrés.";
            case DOCUMENT_REJECTED -> {
                String label = documentLabel(payload);
                String comment = stringVal(payload.get("comment"));
                yield comment != null
                        ? label + " : " + comment
                        : label + " doit être remplacé ou complété.";
            }
            case DOCUMENT_VALIDATED -> documentLabel(payload) + " a été validé par le conseiller.";
            case APPLICATION_APPROVED -> {
                String comment = stringVal(payload.get("comment"));
                if (comment != null) {
                    yield comment;
                } else if (loan.getDecisionComment() != null) {
                    yield loan.getDecisionComment();
                } else {
                    yield "Votre demande a été acceptée.";
                }
            }
            case APPLICATION_REJECTED -> {
                String comment = stringVal(payload.get("comment"));
                if (comment != null) {
                    yield comment;
                } else if (loan.getDecisionComment() != null) {
                    yield loan.getDecisionComment();
                } else {
                    yield "Votre demande n'a pas pu être acceptée.";
                }
            }
            case APPLICATION_CANCELLED -> {
                String comment = stringVal(payload.get("comment"));
                if (comment != null) {
                    yield comment;
                } else if (loan.getDecisionComment() != null) {
                    yield loan.getDecisionComment();
                } else {
                    yield "Cette demande a été annulée.";
                }
            }
            case FUNDS_RELEASED -> "Les fonds sont disponibles sur votre compte.";
        };
    }

    private String resolveState(LoanApplicationEvent event, LoanApplication loan, int index, int total) {
        return switch (event.getEventType()) {
            case DOCUMENT_REJECTED, APPLICATION_REJECTED, APPLICATION_CANCELLED -> "warn";
            case REVIEW_STARTED -> loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW && index == total - 1
                    ? "current"
                    : "done";
            case APPLICATION_APPROVED -> loan.getStatus() == LoanApplicationStatus.APPROVED
                    && loan.getDecidedAt() != null
                    && index >= total - 2
                    ? "done"
                    : "done";
            case FUNDS_RELEASED -> loan.getStatus() == LoanApplicationStatus.APPROVED ? "current" : "done";
            default -> "done";
        };
    }

    private String documentLabel(Map<String, Object> payload) {
        Object type = payload.get("documentType");
        if (type == null) return "Document";
        try {
            LoanDocumentType docType = LoanDocumentType.valueOf(type.toString());
            return DOCUMENT_LABELS.getOrDefault(docType, type.toString());
        } catch (IllegalArgumentException e) {
            return type.toString();
        }
    }

    private String stringVal(Object o) {
        return o == null ? null : o.toString();
    }

    private int intVal(Object o, int defaultValue) {
        if (o instanceof Number number) {
            return number.intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(o.toString());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String bundledDocumentsDescription(int documentCount, int requiredCount) {
        return "Les " + requiredCount + " documents obligatoires ont été déposés"
                + (documentCount > requiredCount
                ? " (" + documentCount + " fichier(s) au total, pièces complémentaires incluses)."
                : ".");
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser le payload d'événement", e);
        }
    }

    private Map<String, Object> deserializePayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

}
