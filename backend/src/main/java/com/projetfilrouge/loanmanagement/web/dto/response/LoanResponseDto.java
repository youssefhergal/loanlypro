package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponseDto {

    private Long id;
    private String reference;
    private LoanApplicationStatus status;

    // Informations demandées
    private BigDecimal requestedAmount;
    private Integer requestedDurationMonths;
    private String purpose;

    // Profil financier
    private BigDecimal monthlyIncome;
    private EmploymentStatus employmentStatus;

    // Suivi temporel
    private Instant submittedAt;
    private Instant decidedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Décision (si applicable)
    private String decisionComment;
    private BigDecimal approvedAmount;
    private Integer approvedDurationMonths;
    private BigDecimal interestRate;

    private Long applicantId;
    private String applicantName;

    private Long advisorId;
    private String advisorName;
}