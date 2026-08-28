package com.projetfilrouge.loanmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.notification.LoanApplicationEventRecorded;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoanApplicationHistoryService {

    private static final Set<LoanDocumentType> REQUIRED_DOCUMENT_TYPES = EnumSet.of(
            LoanDocumentType.IDENTITY,
            LoanDocumentType.PAYSLIPS,
            LoanDocumentType.TAX_NOTICE,
            LoanDocumentType.BANK_STATEMENTS,
            LoanDocumentType.PROOF_OF_ADDRESS
    );

    private static final String STATE_UPCOMING = "upcoming";
    private static final String STATE_CURRENT = "current";
    private static final String STATE_DONE = "done";
    private static final String STATE_WARN = "warn";
    private static final String PAYLOAD_KEY_COMMENT = "comment";

    private static final Map<LoanDocumentType, String> DOCUMENT_LABELS = Map.of(
            LoanDocumentType.IDENTITY, "Pièce d'identité",
            LoanDocumentType.PAYSLIPS, "Bulletins de salaire",
            LoanDocumentType.TAX_NOTICE, "Avis d'imposition",
            LoanDocumentType.BANK_STATEMENTS, "Relevés bancaires",
            LoanDocumentType.PROOF_OF_ADDRESS, "Justificatif de domicile",
            LoanDocumentType.OTHER, "Autre document"
    );

    private static final Set<LoanApplicationEventType> REPAYMENT_ONLY_EVENTS = EnumSet.of(
            LoanApplicationEventType.LOAN_CREATED,
            LoanApplicationEventType.MANDATE_ACTIVATED,
            LoanApplicationEventType.MANDATE_REVOKED,
            LoanApplicationEventType.PAYMENT_SUCCEEDED,
            LoanApplicationEventType.PAYMENT_FAILED,
            LoanApplicationEventType.INSTALLMENT_OVERDUE,
            LoanApplicationEventType.LOAN_CLOSED,
            LoanApplicationEventType.LOAN_DEFAULTED
    );

    private static final Set<LoanApplicationEventType> REPAYMENT_PHASE_EVENTS = EnumSet.of(
            LoanApplicationEventType.FUNDS_RELEASED,
            LoanApplicationEventType.LOAN_CREATED,
            LoanApplicationEventType.MANDATE_ACTIVATED,
            LoanApplicationEventType.MANDATE_REVOKED,
            LoanApplicationEventType.PAYMENT_SUCCEEDED,
            LoanApplicationEventType.PAYMENT_FAILED,
            LoanApplicationEventType.INSTALLMENT_OVERDUE,
            LoanApplicationEventType.LOAN_CLOSED,
            LoanApplicationEventType.LOAN_DEFAULTED
    );

    private final LoanApplicationEventRepository eventRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

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
        LoanApplicationEvent saved = eventRepository.save(event);
        applicationEventPublisher.publishEvent(new LoanApplicationEventRecorded(saved));
        return saved;
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
        return getApplicationHistory(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getApplicationHistory(Long loanId) {
        LoanApplication loan = loanApplicationRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        List<LoanHistoryEventResponseDto> result = buildHistoryEvents(loan);
        result.removeIf(event ->
                event.getEventType() != null && REPAYMENT_ONLY_EVENTS.contains(event.getEventType()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getRepaymentHistory(Long loanApplicationId) {
        LoanApplication loan = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        List<LoanApplicationEvent> events =
                eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(loanApplicationId);
        List<LoanHistoryEventResponseDto> result = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            LoanApplicationEvent event = events.get(i);
            if (REPAYMENT_PHASE_EVENTS.contains(event.getEventType())) {
                result.add(toDisplayDto(event, loan, i, events.size()));
            }
        }
        return result;
    }

    private List<LoanHistoryEventResponseDto> buildHistoryEvents(LoanApplication loan) {
        List<LoanApplicationEvent> events =
                eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(loan.getId());
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

    private void appendProjectedEvents(LoanApplication loan, List<LoanHistoryEventResponseDto> result) {
        if (loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW) {
            result.add(LoanHistoryEventResponseDto.builder()
                    .title("Décision du comité")
                    .description("Le comité de crédit rendra sa décision après analyse complète du dossier.")
                    .state(STATE_UPCOMING)
                    .build());
            result.add(LoanHistoryEventResponseDto.builder()
                    .title("Déblocage des fonds")
                    .description("En cas d'approbation, les fonds seront disponibles sous 24 à 48 heures.")
                    .state(STATE_UPCOMING)
                    .build());
        } else if (loan.getStatus() == LoanApplicationStatus.APPROVED) {
            if (loanRepository.existsByLoanApplicationId(loan.getId())) {
                return;
            }
            boolean hasFunds = result.stream()
                    .anyMatch(e -> e.getEventType() == LoanApplicationEventType.FUNDS_RELEASED);
            if (!hasFunds) {
                result.add(LoanHistoryEventResponseDto.builder()
                        .title("Déblocage des fonds")
                        .description("Les fonds seront disponibles sous 24 à 48 heures.")
                        .state(STATE_CURRENT)
                        .build());
            }
        } else if (loan.getStatus() == LoanApplicationStatus.SUBMITTED) {
            result.add(LoanHistoryEventResponseDto.builder()
                    .title("Analyse du dossier")
                    .description("Un conseiller va examiner votre dossier sous 2 à 5 jours ouvrés.")
                    .state(STATE_UPCOMING)
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
        String title = resolveTitle(event, payload);
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
            return stringVal(payload.get(PAYLOAD_KEY_COMMENT));
        }
        return null;
    }

    private Boolean complementFromPayload(LoanApplicationEventType type, Map<String, Object> payload) {
        if (type != LoanApplicationEventType.DOCUMENT_UPLOADED) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE.equals(payload.get("complement"));
    }

    private String resolveTitle(LoanApplicationEvent event, Map<String, Object> payload) {
        switch (event.getEventType()) {
            case APPLICATION_CREATED:
                return "Brouillon créé";
            case APPLICATION_SUBMITTED:
                return "Demande soumise";
            case DOCUMENT_UPLOADED:
                return Boolean.TRUE.equals(payload.get("bundled"))
                        ? "Pièces justificatives déposées"
                        : "Document déposé";
            case ADVISOR_ASSIGNED:
                return "Conseiller affecté";
            case REVIEW_STARTED:
                return "Analyse financière";
            case DOCUMENT_REJECTED:
                return "Complément demandé";
            case DOCUMENT_VALIDATED:
                return "Document validé";
            case OFFER_PROPOSED:
                return "Contre-offre proposée";
            case OFFER_ACCEPTED:
                return "Contre-offre acceptée";
            case OFFER_REJECTED:
                return "Contre-offre refusée";
            case APPLICATION_APPROVED:
                return "Demande approuvée";
            case APPLICATION_REJECTED:
                return "Demande refusée";
            case APPLICATION_CANCELLED:
                return "Demande annulée";
            case FUNDS_RELEASED:
                return "Fonds débloqués";
            case LOAN_CREATED:
                return "Plan de remboursement généré";
            case MANDATE_ACTIVATED:
                return "Mandat de prélèvement activé";
            case MANDATE_REVOKED:
                return "Mandat de prélèvement révoqué";
            case PAYMENT_SUCCEEDED:
                return "Prélèvement réussi";
            case PAYMENT_FAILED:
                return "Échec de prélèvement";
            case INSTALLMENT_OVERDUE:
                return "Échéance en retard";
            case LOAN_CLOSED:
                return "Prêt soldé";
            case LOAN_DEFAULTED:
                return "Prêt en défaut";
            default:
                return "Événement";
        }
    }

    private String resolveDescription(LoanApplicationEvent event, Map<String, Object> payload, LoanApplication loan) {
        switch (event.getEventType()) {
            case APPLICATION_CREATED:
                return "Votre demande a été enregistrée. Référence : #" + loan.getReference() + ".";
            case APPLICATION_SUBMITTED:
                return "Votre dossier a été transmis pour étude. Référence : #" + loan.getReference() + ".";
            case DOCUMENT_UPLOADED:
                return describeDocumentUploaded(payload);
            case ADVISOR_ASSIGNED:
                return describeAdvisorAssigned(event, payload);
            case REVIEW_STARTED:
                return "Votre dossier est en cours d'analyse. Durée estimée : 2 à 5 jours ouvrés.";
            case DOCUMENT_REJECTED:
                String rejectLabel = documentLabel(payload);
                String rejectComment = stringVal(payload.get(PAYLOAD_KEY_COMMENT));
                return rejectComment != null
                        ? rejectLabel + " : " + rejectComment
                        : rejectLabel + " doit être remplacé ou complété.";
            case DOCUMENT_VALIDATED:
                return documentLabel(payload) + " a été validé par le conseiller.";
            case OFFER_PROPOSED:
                String offerMsg = stringVal(payload.get("clientMessage"));
                return offerMsg != null && !offerMsg.isBlank()
                        ? "Votre conseiller vous propose une nouvelle offre : " + offerMsg
                        : "Votre conseiller vous a proposé une contre-offre. Merci de l'accepter ou de la refuser.";
            case OFFER_ACCEPTED:
                return "Vous avez accepté la contre-offre proposée par votre conseiller.";
            case OFFER_REJECTED:
                String offerRejectComment = stringVal(payload.get(PAYLOAD_KEY_COMMENT));
                return offerRejectComment != null && !offerRejectComment.isBlank()
                        ? "Vous avez refusé la contre-offre : " + offerRejectComment
                        : "Vous avez refusé la contre-offre proposée.";
            case APPLICATION_APPROVED:
                return resolveDecisionDescription(payload, loan, "Votre demande a été acceptée.");
            case APPLICATION_REJECTED:
                return resolveDecisionDescription(payload, loan, "Votre demande n'a pas pu être acceptée.");
            case APPLICATION_CANCELLED:
                return resolveDecisionDescription(payload, loan, "Cette demande a été annulée.");
            case FUNDS_RELEASED:
                return "Les fonds sont disponibles sur votre compte.";
            case LOAN_CREATED:
                int count = intVal(payload.get("installmentCount"), 0);
                return "Votre plan de remboursement a été créé (" + count + " échéances). "
                        + "Configurez votre mandat de prélèvement pour activer les débits automatiques.";
            case MANDATE_ACTIVATED:
                return "Votre mandat SEPA est actif. Les prélèvements automatiques peuvent démarrer.";
            case MANDATE_REVOKED:
                return "Votre mandat de prélèvement a été révoqué. Réactivez-le pour reprendre les débits.";
            case PAYMENT_SUCCEEDED:
                return "Prélèvement de " + payload.get("amount") + " € effectué avec succès.";
            case PAYMENT_FAILED:
                String reason = stringVal(payload.get("failureReason"));
                return reason != null
                        ? "Échec du prélèvement : " + reason + "."
                        : "Échec du prélèvement. Une nouvelle tentative sera planifiée.";
            case INSTALLMENT_OVERDUE:
                return "L'échéance n°" + payload.get("sequenceNumber")
                        + " est en retard après plusieurs tentatives de prélèvement.";
            case LOAN_CLOSED:
                return "Félicitations, votre prêt est entièrement remboursé.";
            case LOAN_DEFAULTED:
                return "Votre prêt est passé en défaut de paiement. Contactez votre conseiller.";
            default:
                return "";
        }
    }

    private String describeDocumentUploaded(Map<String, Object> payload) {
        if (Boolean.TRUE.equals(payload.get("bundled"))) {
            int count = intVal(payload.get("documentCount"), REQUIRED_DOCUMENT_TYPES.size());
            return bundledDocumentsDescription(count, REQUIRED_DOCUMENT_TYPES.size());
        }
        String uploadLabel = documentLabel(payload);
        String file = stringVal(payload.get("fileName"));
        boolean complement = Boolean.TRUE.equals(payload.get("complement"));
        String prefix = complement ? "Nouveau dépôt — " : "";
        return file != null ? prefix + uploadLabel + " — " + file : prefix + uploadLabel + " reçu.";
    }

    private String describeAdvisorAssigned(LoanApplicationEvent event, Map<String, Object> payload) {
        String advisorName = stringVal(payload.get("advisorName"));
        if (advisorName != null && !advisorName.isBlank()) {
            return advisorName + " est désormais en charge de votre dossier.";
        }
        String actorName = event.getActorDisplayName();
        return actorName != null && !actorName.isBlank()
                ? actorName + " est désormais en charge de votre dossier."
                : "Un conseiller a été affecté à votre dossier.";
    }

    private String resolveDecisionDescription(
            Map<String, Object> payload,
            LoanApplication loan,
            String fallback
    ) {
        String comment = stringVal(payload.get(PAYLOAD_KEY_COMMENT));
        if (comment != null) {
            return comment;
        }
        if (loan.getDecisionComment() != null) {
            return loan.getDecisionComment();
        }
        return fallback;
    }

    private String resolveState(LoanApplicationEvent event, LoanApplication loan, int index, int total) {
        return switch (event.getEventType()) {
            case DOCUMENT_REJECTED, APPLICATION_REJECTED, APPLICATION_CANCELLED -> STATE_WARN;
            case REVIEW_STARTED -> loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW && index == total - 1
                    ? STATE_CURRENT
                    : STATE_DONE;
            case APPLICATION_APPROVED -> loan.getStatus() == LoanApplicationStatus.APPROVED
                    && loan.getDecidedAt() != null
                    && index == total - 1
                    ? STATE_CURRENT
                    : STATE_DONE;
            case FUNDS_RELEASED -> loan.getStatus() == LoanApplicationStatus.APPROVED ? STATE_CURRENT : STATE_DONE;
            default -> STATE_DONE;
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
