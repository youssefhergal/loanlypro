package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repayment_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @Column(name = "total_repayable", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRepayable;

    @Column(name = "monthly_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;

    @OneToMany(mappedBy = "repaymentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;
}
