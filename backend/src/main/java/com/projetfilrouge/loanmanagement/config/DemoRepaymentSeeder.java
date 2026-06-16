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
import com.projetfilrouge.loanmanagement.service.MandateService;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Jeu de données démo : prêt LF-DEMO-0001 actif avec historique d'échéances.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("demo")
@Order(30)
public class DemoRepaymentSeeder implements CommandLineRunner {

    private static final String DEMO_REFERENCE = "LF-DEMO-0001";
    private static final String CLIENT_EMAIL = "client@test.com";
    private static final String ADVISOR_EMAIL = "conseiller@test.com";
    private static final String DEMO_IBAN = "FR1420041010050500013M02606";

    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final RepaymentPlanService repaymentPlanService;
    private final MandateService mandateService;

    @Override
    @Transactional
    public void run(String... args) {
        var client = userRepository.findByEmail(CLIENT_EMAIL).orElse(null);
        var advisor = userRepository.findByEmail(ADVISOR_EMAIL).orElse(null);
        if (client == null || advisor == null) {
            log.warn("Demo repayment seed ignoré : comptes client/conseiller manquants.");
            return;
        }

        LoanApplication application = loanApplicationRepository.findByReference(DEMO_REFERENCE)
                .orElseGet(() -> createApprovedApplication(client, advisor));

        Loan loan = loanRepository.findByLoanApplicationId(application.getId())
                .orElseGet(() -> repaymentPlanService.createLoanFromApprovedApplication(application));

        if (loan.getStatus() == LoanStatus.PENDING_MANDATE || !mandateService.hasActiveMandate(loan.getId())) {
            mandateService.registerPaymentMethodAndActivateMandate(
                    loan.getId(),
                    CLIENT_EMAIL,
                    DEMO_IBAN,
                    client.getFirstName() + " " + client.getLastName()
            );
            loan = loanRepository.findById(loan.getId()).orElse(loan);
        }

        tuneInstallmentsForDemo(loan);
        log.info("Demo repayment seed : prêt {} prêt pour la démo.", DEMO_REFERENCE);
    }

    private LoanApplication createApprovedApplication(
            com.projetfilrouge.loanmanagement.entity.User client,
            com.projetfilrouge.loanmanagement.entity.User advisor
    ) {
        LoanApplication application = LoanApplication.builder()
                .reference(DEMO_REFERENCE)
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.APPROVED)
                .requestedAmount(new BigDecimal("15000.00"))
                .requestedDurationMonths(48)
                .title("Prêt personnel démo")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet personnel")
                .monthlyIncome(new BigDecimal("3500.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .approvedAmount(new BigDecimal("15000.00"))
                .approvedDurationMonths(48)
                .interestRate(new BigDecimal("3.85"))
                .submittedAt(Instant.now().minusSeconds(86400L * 30))
                .decidedAt(Instant.now().minusSeconds(86400L * 7))
                .build();
        return loanApplicationRepository.save(application);
    }

    private void tuneInstallmentsForDemo(Loan loan) {
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan == null) {
            return;
        }

        List<Installment> installments = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
        if (installments.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        BigDecimal runningBalance = loan.getPrincipalAmount();

        for (int i = 0; i < installments.size(); i++) {
            Installment installment = installments.get(i);
            installment.setDueDate(today.minusMonths(3L - i).withDayOfMonth(Math.min(today.getDayOfMonth(), 28)));

            if (i < 2) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setAttemptCount(1);
                installment.setNextRetryDate(null);
                runningBalance = installment.getRemainingBalance();
            } else if (i == 2) {
                installment.setStatus(InstallmentStatus.FAILED);
                installment.setAttemptCount(1);
                installment.setNextRetryDate(today.plusDays(3));
            } else {
                installment.setStatus(InstallmentStatus.UPCOMING);
                installment.setAttemptCount(0);
                installment.setNextRetryDate(null);
            }
            installmentRepository.save(installment);
        }

        loan.setRemainingBalance(runningBalance);
        loan.setStatus(LoanStatus.ACTIVE);
        if (loan.getActivatedAt() == null) {
            loan.setActivatedAt(Instant.now().minusSeconds(86400L * 20));
        }
        loanRepository.save(loan);
    }
}
