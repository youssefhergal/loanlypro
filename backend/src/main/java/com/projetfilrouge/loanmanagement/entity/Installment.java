package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repayment_plan_id", nullable = false)
    private RepaymentPlan repaymentPlan;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "principal_part", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPart;

    @Column(name = "interest_part", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPart;

    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InstallmentStatus status;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_retry_date")
    private LocalDate nextRetryDate;
}
