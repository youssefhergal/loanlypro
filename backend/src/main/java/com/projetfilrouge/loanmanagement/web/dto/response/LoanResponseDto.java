package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
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

    private String title;
    private LoanPurpose loanPurpose;
    private BigDecimal requestedAmount;
    private Integer requestedDurationMonths;
    private String purpose;
    private String comment;

    private BigDecimal monthlyIncome;
    private EmploymentStatus employmentStatus;
    private BigDecimal additionalIncome;
    private String employerName;
    private Integer seniorityMonths;
    private BigDecimal monthlyRent;
    private BigDecimal monthlyLoanPayments;
    private BigDecimal monthlyAlimony;
    private BigDecimal monthlyOtherCharges;

    private Instant submittedAt;
    private Instant decidedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private String decisionComment;
    private BigDecimal approvedAmount;
    private Integer approvedDurationMonths;
    private BigDecimal interestRate;

    private Long applicantId;
    private String applicantName;

    private Long advisorId;
    private String advisorName;
}
