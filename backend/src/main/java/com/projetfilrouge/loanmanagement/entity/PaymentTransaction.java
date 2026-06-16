package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_tx_installment_attempt",
                columnNames = {"installment_id", "attempt_number"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private Installment installment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentTransactionStatus status;

    @Column(name = "failure_reason", length = 120)
    private String failureReason;

    @Column(name = "external_reference", length = 64)
    private String externalReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
