package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorDashboardResponse {
    private long totalCount;
    private List<LoanSummaryDto> loans;
    // Ajout pour vérifier et exposer les LoanApplication liées au conseiller
    private long applicationsCount;
    private List<LoanApplicationSummaryDto> applications;
}
