package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String reference;

    // Liaison avec le client (Applicant) - Relation 1 côté User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    // Liaison avec le conseiller (Advisor) - Relation 0..1 côté User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    private User assignedAdvisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanApplicationStatus status;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_duration_months", nullable = false)
    private Integer requestedDurationMonths;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_purpose", nullable = false, length = 40)
    private LoanPurpose loanPurpose;

    @Column(nullable = false, length = 255)
    private String purpose;

    @Column(length = 500)
    private String comment;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "additional_income", precision = 15, scale = 2)
    private BigDecimal additionalIncome;

    @Column(name = "employer_name", length = 120)
    private String employerName;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "employer_sector", length = 40)
    private String employerSector;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "seniority_months")
    private Integer seniorityMonths;

    @Column(name = "monthly_rent", precision = 15, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "monthly_loan_payments", precision = 15, scale = 2)
    private BigDecimal monthlyLoanPayments;

    @Column(name = "monthly_alimony", precision = 15, scale = 2)
    private BigDecimal monthlyAlimony;

    @Column(name = "monthly_other_charges", precision = 15, scale = 2)
    private BigDecimal monthlyOtherCharges;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 40)
    private EmploymentStatus employmentStatus;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_comment")
    private String decisionComment;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "approved_duration_months")
    private Integer approvedDurationMonths;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}