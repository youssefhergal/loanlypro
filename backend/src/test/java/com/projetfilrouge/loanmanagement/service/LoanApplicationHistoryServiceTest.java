package com.projetfilrouge.loanmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationHistoryServiceTest {

    @Mock
    private LoanApplicationEventRepository eventRepository;

    @Mock
    private LoanApplicationRepository loanRepository;

    private LoanApplicationHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new LoanApplicationHistoryService(
                eventRepository,
                loanRepository,
                new ObjectMapper()
        );
    }

    @Test
    void recordEvent_persistsEventWithSerializedPayload() {
        LoanApplication loan = sampleLoan(LoanApplicationStatus.DRAFT);
        Instant occurredAt = Instant.parse("2026-01-15T10:00:00Z");

        when(eventRepository.save(any(LoanApplicationEvent.class))).thenAnswer(invocation -> {
            LoanApplicationEvent saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        LoanApplicationEvent result = historyService.recordEvent(
                loan,
                LoanApplicationEventType.APPLICATION_CREATED,
                LoanEventActorType.CLIENT,
                "client@test.com",
                "Jean Dupont",
                Map.of("reference", "LOAN-ABC"),
                occurredAt
        );

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getEventType()).isEqualTo(LoanApplicationEventType.APPLICATION_CREATED);
        assertThat(result.getPayloadJson()).contains("LOAN-ABC");

        ArgumentCaptor<LoanApplicationEvent> captor = ArgumentCaptor.forClass(LoanApplicationEvent.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getActorEmail()).isEqualTo("client@test.com");
        assertThat(captor.getValue().getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void getHistory_throwsWhenLoanNotFound() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.getHistory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Dossier introuvable");
    }

    @Test
    void getHistory_appendsSubmittedProjection() {
        LoanApplication loan = sampleLoan(LoanApplicationStatus.SUBMITTED);
        LoanApplicationEvent created = event(
                1L,
                loan,
                LoanApplicationEventType.APPLICATION_CREATED,
                "{\"reference\":\"LOAN-TEST\"}",
                Instant.parse("2026-01-10T09:00:00Z")
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(created));

        List<LoanHistoryEventResponseDto> history = historyService.getHistory(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTitle()).isEqualTo("Brouillon créé");
        assertThat(history.get(1).getTitle()).isEqualTo("Analyse du dossier");
        assertThat(history.get(1).getState()).isEqualTo("upcoming");
    }

    @Test
    void getHistory_appendsUnderReviewProjections() {
        LoanApplication loan = sampleLoan(LoanApplicationStatus.UNDER_REVIEW);
        LoanApplicationEvent reviewStarted = event(
                2L,
                loan,
                LoanApplicationEventType.REVIEW_STARTED,
                "{}",
                Instant.parse("2026-01-12T11:00:00Z")
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(reviewStarted));

        List<LoanHistoryEventResponseDto> history = historyService.getHistory(1L);

        assertThat(history).hasSize(3);
        assertThat(history.get(0).getTitle()).isEqualTo("Analyse financière");
        assertThat(history.get(1).getTitle()).isEqualTo("Décision du comité");
        assertThat(history.get(2).getTitle()).isEqualTo("Déblocage des fonds");
        assertThat(history.get(1).getState()).isEqualTo("upcoming");
    }

    @Test
    void getHistory_consolidatesMultipleDocumentUploadEvents() {
        LoanApplication loan = sampleLoan(LoanApplicationStatus.SUBMITTED);
        LoanApplicationEvent uploadIdentity = event(
                10L,
                loan,
                LoanApplicationEventType.DOCUMENT_UPLOADED,
                "{\"documentType\":\"IDENTITY\",\"fileName\":\"id.pdf\"}",
                Instant.parse("2026-01-11T08:00:00Z")
        );
        LoanApplicationEvent uploadPayslips = event(
                11L,
                loan,
                LoanApplicationEventType.DOCUMENT_UPLOADED,
                "{\"documentType\":\"PAYSLIPS\",\"fileName\":\"payslips.pdf\"}",
                Instant.parse("2026-01-11T08:01:00Z")
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(uploadIdentity, uploadPayslips));

        List<LoanHistoryEventResponseDto> history = historyService.getHistory(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTitle()).isEqualTo("Pièces justificatives déposées");
        assertThat(history.get(0).getDescription())
                .contains("5 documents obligatoires");
        assertThat(history.get(1).getTitle()).isEqualTo("Analyse du dossier");
    }

    @Test
    void mapEventToDisplayDto_resolvesRejectedDocumentDetails() {
        LoanApplication loan = sampleLoan(LoanApplicationStatus.UNDER_REVIEW);
        LoanApplicationEvent rejected = event(
                5L,
                loan,
                LoanApplicationEventType.DOCUMENT_REJECTED,
                "{\"documentType\":\"IDENTITY\",\"comment\":\"Photo floue\"}",
                Instant.parse("2026-01-13T14:00:00Z")
        );
        rejected.setActorDisplayName("Marie Conseil");

        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(rejected));

        LoanHistoryEventResponseDto dto = historyService.mapEventToDisplayDto(rejected);

        assertThat(dto.getTitle()).isEqualTo("Complément demandé");
        assertThat(dto.getDescription()).isEqualTo("Pièce d'identité : Photo floue");
        assertThat(dto.getDocumentType()).isEqualTo(LoanDocumentType.IDENTITY);
        assertThat(dto.getComment()).isEqualTo("Photo floue");
        assertThat(dto.getState()).isEqualTo("warn");
    }

    @Test
    void getHistory_appendsFundsProjectionWhenApproved() {
        LoanApplication loan = sampleLoan(LoanApplicationStatus.APPROVED);
        loan.setDecidedAt(Instant.parse("2026-01-20T16:00:00Z"));
        LoanApplicationEvent approved = event(
                20L,
                loan,
                LoanApplicationEventType.APPLICATION_APPROVED,
                "{\"comment\":\"Dossier accepté\"}",
                Instant.parse("2026-01-20T16:00:00Z")
        );

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(eventRepository.findByLoanApplicationIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(approved));

        List<LoanHistoryEventResponseDto> history = historyService.getHistory(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTitle()).isEqualTo("Demande approuvée");
        assertThat(history.get(0).getDescription()).isEqualTo("Dossier accepté");
        assertThat(history.get(1).getTitle()).isEqualTo("Déblocage des fonds");
        assertThat(history.get(1).getState()).isEqualTo("current");
    }

    private static LoanApplication sampleLoan(LoanApplicationStatus status) {
        return LoanApplication.builder()
                .id(1L)
                .reference("LOAN-TEST")
                .status(status)
                .build();
    }

    private static LoanApplicationEvent event(
            Long id,
            LoanApplication loan,
            LoanApplicationEventType type,
            String payloadJson,
            Instant occurredAt
    ) {
        return LoanApplicationEvent.builder()
                .id(id)
                .loanApplication(loan)
                .eventType(type)
                .payloadJson(payloadJson)
                .occurredAt(occurredAt)
                .actorType(LoanEventActorType.CLIENT)
                .actorEmail("client@test.com")
                .actorDisplayName("Jean Dupont")
                .build();
    }
}
