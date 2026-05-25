package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanHistoryEventResponseDto {

    private Long id;
    private LoanApplicationEventType eventType;
    private Instant occurredAt;
    private LoanEventActorType actorType;
    private String actorDisplayName;
    private String title;
    private String description;
    /** done | current | upcoming | warn */
    private String state;
    /** Présent pour DOCUMENT_REJECTED, DOCUMENT_VALIDATED, DOCUMENT_UPLOADED */
    private LoanDocumentType documentType;
    private String comment;
    /** true si DOCUMENT_UPLOADED avec complément client */
    private Boolean complement;
}
