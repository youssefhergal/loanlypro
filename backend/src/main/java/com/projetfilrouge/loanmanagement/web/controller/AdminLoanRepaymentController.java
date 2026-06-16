package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.service.RepaymentQueryService;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDetailDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.RepaymentKpiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/loans")
@RequiredArgsConstructor
@Tag(name = "Admin Loan Repayment", description = "Supervision des prêts et KPI recouvrement (lecture seule)")
public class AdminLoanRepaymentController {

    private final RepaymentQueryService repaymentQueryService;

    @GetMapping("/kpi")
    @Operation(summary = "KPI recouvrement", description = "Indicateurs agrégés du portefeuille de prêts.")
    public ResponseEntity<RepaymentKpiDto> getKpi(Authentication authentication) {
        return ResponseEntity.ok(repaymentQueryService.getAdminKpi(authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Tous les prêts", description = "Liste paginée de l'ensemble du portefeuille.")
    public ResponseEntity<Page<LoanSummaryDto>> getAllLoans(
            Authentication authentication,
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(repaymentQueryService.getAdminLoans(authentication.getName(), status, pageable));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Détail prêt admin", description = "Détail complet avec échéances et transactions.")
    public ResponseEntity<LoanDetailDto> getLoanDetail(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repaymentQueryService.getLoanDetailForAdmin(loanId, authentication.getName()));
    }

    @GetMapping("/{loanId}/history")
    @Operation(summary = "Historique remboursement", description = "Timeline des événements liés au prêt.")
    public ResponseEntity<List<LoanHistoryEventResponseDto>> getHistory(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                repaymentQueryService.getRepaymentHistoryForAdmin(loanId, authentication.getName())
        );
    }
}
