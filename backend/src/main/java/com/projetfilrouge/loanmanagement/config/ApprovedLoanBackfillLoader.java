package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.service.RepaymentPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crée les prêts de remboursement manquants pour les demandes déjà APPROVED
 * (ex. approuvées avant l'activation du module prêt).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(20)
public class ApprovedLoanBackfillLoader implements CommandLineRunner {

    private final RepaymentPlanService repaymentPlanService;

    @Override
    public void run(String... args) {
        int created = repaymentPlanService.backfillLoansForApprovedApplications();
        if (created > 0) {
            log.info("Rattrapage prêts : {} prêt(s) créé(s) pour des demandes APPROVED sans prêt.", created);
        }
    }
}
