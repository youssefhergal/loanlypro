package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.service.RepaymentPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Profil D dashboard : prêt LF-DEMO-0000 soldé pour pierre.client@test.com.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("demo")
@Order(25)
public class DemoClosedLoanSeeder implements CommandLineRunner {

    private static final String CLIENT_EMAIL = "pierre.client@test.com";
    private static final String ADVISOR_EMAIL = "conseiller@test.com";
    private static final String CLOSED_REFERENCE = "LF-DEMO-0000";
    private static final String ARCHIVED_REFERENCE = "LF-DEMO-D002";

    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final RepaymentPlanService repaymentPlanService;

    @Override
    @Transactional
    public void run(String... args) {
        var client = userRepository.findByEmail(CLIENT_EMAIL).orElse(null);
        var advisor = userRepository.findByEmail(ADVISOR_EMAIL).orElse(null);
        if (client == null || advisor == null) {
            log.warn("Demo closed loan seed ignoré : comptes client/conseiller manquants.");
            return;
        }

        if (loanApplicationRepository.findByReference(CLOSED_REFERENCE).isPresent()) {
            log.info("Demo closed loan seed : dossier {} déjà présent, skip.", CLOSED_REFERENCE);
            return;
        }

        Instant now = Instant.now();

        LoanApplication closedApplication = loanApplicationRepository.save(LoanApplication.builder()
                .reference(CLOSED_REFERENCE)
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.APPROVED)
                .requestedAmount(new BigDecimal("10000.00"))
                .requestedDurationMonths(20)
                .title("Prêt personnel — soldé")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet personnel remboursé intégralement")
                .monthlyIncome(new BigDecimal("3400.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .approvedAmount(new BigDecimal("10000.00"))
                .approvedDurationMonths(20)
                .interestRate(new BigDecimal("3.50"))
                .submittedAt(now.minus(700, ChronoUnit.DAYS))
                .decidedAt(now.minus(680, ChronoUnit.DAYS))
                .offerClientAccepted(true)
                .build());

        loanApplicationRepository.save(LoanApplication.builder()
                .reference(ARCHIVED_REFERENCE)
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.REJECTED)
                .requestedAmount(new BigDecimal("5000.00"))
                .requestedDurationMonths(24)
                .title("Ancienne demande refusée")
                .loanPurpose(LoanPurpose.VEHICLE)
                .purpose("Véhicule — dossier archivé")
                .monthlyIncome(new BigDecimal("3400.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .submittedAt(now.minus(900, ChronoUnit.DAYS))
                .decidedAt(now.minus(880, ChronoUnit.DAYS))
                .build());

        Loan loan = repaymentPlanService.createLoanFromApprovedApplication(closedApplication);
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan != null) {
            List<Installment> installments = installmentRepository
                    .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
            BigDecimal zero = BigDecimal.ZERO.setScale(2);
            for (Installment installment : installments) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setAttemptCount(1);
                installment.setNextRetryDate(null);
                installmentRepository.save(installment);
            }
            loan.setRemainingBalance(zero);
            loan.setStatus(LoanStatus.CLOSED);
            loan.setActivatedAt(now.minus(670, ChronoUnit.DAYS));
            loan.setClosedAt(now.minus(30, ChronoUnit.DAYS));
            loanRepository.save(loan);
        }

        log.info("Demo closed loan seed : prêt {} CLOSED pour {}.", CLOSED_REFERENCE, CLIENT_EMAIL);
    }
}
