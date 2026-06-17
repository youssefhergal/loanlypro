package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.service.RepaymentQueryService;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDetailDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanSummaryDto;
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
@RequestMapping("/api/v1/advisor/loans")
@RequiredArgsConstructor
@Tag(name = "Advisor Loan Repayment", description = "Consultation des prêts en cours (lecture seule)")
public class AdvisorLoanRepaymentController {

    private final RepaymentQueryService repaymentQueryService;

    @GetMapping
    @Operation(summary = "Prêts des clients affectés", description = "Liste paginée des prêts supervisés par le conseiller.")
    public ResponseEntity<Page<LoanSummaryDto>> getAdvisorLoans(
            Authentication authentication,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean overdueOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(repaymentQueryService.getAdvisorLoans(
                authentication.getName(),
                status,
                overdueOnly,
                pageable
        ));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Détail prêt conseiller", description = "Détail complet avec échéances et transactions.")
    public ResponseEntity<LoanDetailDto> getAdvisorLoanDetail(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repaymentQueryService.getLoanDetailForAdvisor(loanId, authentication.getName()));
    }

    @GetMapping("/{loanId}/history")
    @Operation(summary = "Historique prêt conseiller", description = "Événements de remboursement du prêt supervisé.")
    public ResponseEntity<List<LoanHistoryEventResponseDto>> getAdvisorLoanHistory(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                repaymentQueryService.getRepaymentHistoryForAdvisor(loanId, authentication.getName())
        );
    }
}
