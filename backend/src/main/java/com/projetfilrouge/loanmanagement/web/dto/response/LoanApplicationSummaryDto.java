package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationSummaryDto {
    private Long id;
    private String reference;
    private LoanApplicationStatus status;
    private String title;
    private BigDecimal requestedAmount;
    private Integer requestedDurationMonths;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant decidedAt;
}
