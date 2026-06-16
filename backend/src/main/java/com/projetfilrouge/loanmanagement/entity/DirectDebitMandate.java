package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "direct_debit_mandates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectDebitMandate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "mandate_reference", nullable = false, unique = true, length = 64)
    private String mandateReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MandateStatus status;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
