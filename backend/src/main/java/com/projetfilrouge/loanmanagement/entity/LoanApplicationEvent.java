package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "loan_application_events", indexes = {
        @Index(name = "idx_loan_events_loan_occurred", columnList = "loan_application_id, occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private LoanApplicationEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private LoanEventActorType actorType;

    @Column(name = "actor_email", length = 120)
    private String actorEmail;

    @Column(name = "actor_display_name", length = 120)
    private String actorDisplayName;

    /** JSON libre : documentType, comment, fileName, etc. */
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;
}
