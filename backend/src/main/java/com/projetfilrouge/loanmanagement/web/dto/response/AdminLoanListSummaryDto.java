package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoanListSummaryDto {
    private long totalCount;
    private long unassignedCount;
    private Map<LoanApplicationStatus, Long> statusCounts;
    private List<AdminAdvisorOptionDto> advisors;
}
