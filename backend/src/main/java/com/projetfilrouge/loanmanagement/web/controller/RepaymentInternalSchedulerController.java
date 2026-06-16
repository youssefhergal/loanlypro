package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.RepaymentSchedulerService;
import com.projetfilrouge.loanmanagement.security.SchedulerAccessService;
import com.projetfilrouge.loanmanagement.web.dto.response.RepaymentSchedulerRunResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/internal/scheduler")
@RequiredArgsConstructor
@Tag(name = "Repayment Scheduler", description = "Déclenchement manuel des prélèvements (dev / admin)")
public class RepaymentInternalSchedulerController {

    private final RepaymentSchedulerService repaymentSchedulerService;
    private final SchedulerAccessService schedulerAccessService;

    @PostMapping("/run-due-installments")
    @Operation(summary = "Exécuter les prélèvements dus", description = "Réservé admin ou token interne (X-Scheduler-Token).")
    public ResponseEntity<RepaymentSchedulerRunResultDto> runDueInstallments(
            @RequestHeader(value = "X-Scheduler-Token", required = false) String schedulerToken,
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        schedulerAccessService.ensureCanRunScheduler(schedulerToken, authentication);
        LocalDate processingDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(repaymentSchedulerService.processDueInstallments(processingDate));
    }
}
