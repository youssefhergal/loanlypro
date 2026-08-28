package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.service.MandateService;
import com.projetfilrouge.loanmanagement.service.RepaymentQueryService;
import com.projetfilrouge.loanmanagement.web.dto.request.ActivateMandateRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.InstallmentDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDetailDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.MandateResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.PaymentTransactionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Repayment", description = "Prêt en cours et mandat de prélèvement")
public class LoanRepaymentController {

    private final MandateService mandateService;
    private final RepaymentQueryService repaymentQueryService;

    @GetMapping("/me")
    @Operation(summary = "Mes prêts", description = "Liste des prêts du client connecté.")
    public ResponseEntity<List<LoanSummaryDto>> getMyLoans(Authentication authentication) {
        return ResponseEntity.ok(repaymentQueryService.getMyLoans(authentication.getName()));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Détail d'un prêt", description = "Synthèse du prêt pour le client emprunteur.")
    public ResponseEntity<LoanDetailDto> getLoanDetail(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repaymentQueryService.getLoanDetailForBorrower(loanId, authentication.getName()));
    }

    @GetMapping("/{loanId}/installments")
    @Operation(summary = "Échéancier", description = "Liste complète des échéances du prêt.")
    public ResponseEntity<List<InstallmentDto>> getInstallments(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repaymentQueryService.getInstallmentsForBorrower(loanId, authentication.getName()));
    }

    @GetMapping("/{loanId}/transactions")
    @Operation(summary = "Historique des prélèvements", description = "Transactions de prélèvement du prêt.")
    public ResponseEntity<List<PaymentTransactionDto>> getTransactions(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repaymentQueryService.getTransactionsForBorrower(loanId, authentication.getName()));
    }

    @GetMapping("/{loanId}/history")
    @Operation(summary = "Historique du prêt", description = "Événements à partir du déblocage des fonds.")
    public ResponseEntity<List<LoanHistoryEventResponseDto>> getRepaymentHistory(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                repaymentQueryService.getRepaymentHistoryForBorrower(loanId, authentication.getName())
        );
    }

    @PostMapping("/{loanId}/mandate/activate")
    @Operation(summary = "Activer le mandat de prélèvement", description = "Enregistre l'IBAN et active le mandat SEPA pour le prêt.")
    public ResponseEntity<MandateResponseDto> activateMandate(
            @PathVariable Long loanId,
            @Valid @RequestBody ActivateMandateRequestDto request,
            Authentication authentication
    ) {
        DirectDebitMandate mandate = mandateService.registerPaymentMethodAndActivateMandate(
                loanId,
                authentication.getName(),
                request.getIban(),
                request.getHolderName()
        );
        return ResponseEntity.ok(MandateResponseDto.builder()
                .loanId(loanId)
                .mandateReference(mandate.getMandateReference())
                .status(mandate.getStatus().name())
                .ibanMasked(mandate.getPaymentMethod().getIbanMasked())
                .holderName(mandate.getPaymentMethod().getHolderName())
                .build());
    }

    @PostMapping("/{loanId}/mandate/revoke")
    @Operation(summary = "Révoquer le mandat de prélèvement", description = "Révoque le mandat SEPA actif du prêt.")
    public ResponseEntity<MandateResponseDto> revokeMandate(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        DirectDebitMandate mandate = mandateService.revokeMandate(loanId, authentication.getName());
        return ResponseEntity.ok(MandateResponseDto.builder()
                .loanId(loanId)
                .mandateReference(mandate.getMandateReference())
                .status(mandate.getStatus().name())
                .ibanMasked(mandate.getPaymentMethod().getIbanMasked())
                .holderName(mandate.getPaymentMethod().getHolderName())
                .build());
    }
}
