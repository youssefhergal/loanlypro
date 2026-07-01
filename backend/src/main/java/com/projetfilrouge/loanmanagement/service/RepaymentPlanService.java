package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentPlanService {

    private static final MathContext CALC_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    private static final int MONEY_SCALE = 2;

    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final LoanApplicationHistoryService historyService;
    private final LoanInterestRateService interestRateService;

    @Transactional
    public int backfillLoansForApprovedApplications() {
        List<LoanApplication> orphans = loanApplicationRepository.findApprovedWithoutRepaymentLoan();
        int created = 0;

        for (LoanApplication application : orphans) {
            try {
                ensureApprovalTerms(application);
                createLoanFromApprovedApplication(application);
                created++;
                log.info(
                        "Prêt de remboursement créé pour la demande approuvée {} (id={})",
                        application.getReference(),
                        application.getId()
                );
            } catch (Exception ex) {
                log.warn(
                        "Impossible de créer le prêt pour la demande {} (id={}) : {}",
                        application.getReference(),
                        application.getId(),
                        ex.getMessage()
                );
            }
        }

        return created;
    }

    private void ensureApprovalTerms(LoanApplication application) {
        boolean changed = false;

        if (application.getApprovedAmount() == null) {
            application.setApprovedAmount(application.getRequestedAmount());
            changed = true;
        }
        if (application.getApprovedDurationMonths() == null) {
            application.setApprovedDurationMonths(application.getRequestedDurationMonths());
            changed = true;
        }
        if (application.getInterestRate() == null) {
            application.setInterestRate(
                    interestRateService.calculateIndicativeRate(
                            application.getRequestedAmount(),
                            application.getRequestedDurationMonths(),
                            application.getLoanPurpose()
                    )
            );
            changed = true;
        }

        if (changed) {
            loanApplicationRepository.save(application);
        }
    }

    @Transactional
    public Loan createLoanFromApprovedApplication(LoanApplication application) {
        if (application.getStatus() != LoanApplicationStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Un prêt ne peut être généré que pour une demande approuvée (APPROVED)."
            );
        }

        return loanRepository.findByLoanApplicationId(application.getId())
                .orElseGet(() -> createNewLoan(application));
    }

    private Loan createNewLoan(LoanApplication application) {
        BigDecimal principal = requirePositive(application.getApprovedAmount(), "montant accordé");
        int durationMonths = requirePositiveMonths(application.getApprovedDurationMonths());
        BigDecimal annualRate = requirePositive(application.getInterestRate(), "taux d'intérêt");

        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRate, durationMonths);
        List<Installment> installments = buildInstallments(principal, annualRate, durationMonths, monthlyPayment);

        BigDecimal totalRepayable = installments.stream()
                .map(Installment::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Loan loan = Loan.builder()
                .loanApplication(application)
                .borrower(application.getApplicant())
                .assignedAdvisor(application.getAssignedAdvisor())
                .status(LoanStatus.PENDING_MANDATE)
                .principalAmount(scaleMoney(principal))
                .durationMonths(durationMonths)
                .annualRate(annualRate)
                .remainingBalance(scaleMoney(principal))
                .build();

        Loan savedLoan = loanRepository.save(loan);

        RepaymentPlan plan = RepaymentPlan.builder()
                .loan(savedLoan)
                .totalRepayable(scaleMoney(totalRepayable))
                .monthlyPayment(scaleMoney(monthlyPayment))
                .installmentCount(durationMonths)
                .installments(new ArrayList<>())
                .build();

        for (Installment installment : installments) {
            installment.setRepaymentPlan(plan);
            plan.getInstallments().add(installment);
        }

        repaymentPlanRepository.save(plan);

        historyService.recordEvent(
                application,
                LoanApplicationEventType.LOAN_CREATED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of(
                        "loanId", savedLoan.getId(),
                        "installmentCount", durationMonths,
                        "monthlyPayment", plan.getMonthlyPayment(),
                        "reference", application.getReference()
                )
        );

        return savedLoan;
    }

    BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (months <= 0) {
            throw new BusinessRuleException("La durée du prêt doit être strictement positive.");
        }
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(100), CALC_CONTEXT)
                .divide(BigDecimal.valueOf(12), CALC_CONTEXT);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return scaleMoney(principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.HALF_UP));
        }

        double rate = monthlyRate.doubleValue();
        double factor = Math.pow(1.0 + rate, -months);
        double payment = principal.doubleValue() * rate / (1.0 - factor);
        return scaleMoney(BigDecimal.valueOf(payment));
    }

    List<Installment> buildInstallments(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int months,
            BigDecimal monthlyPayment
    ) {
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(100), CALC_CONTEXT)
                .divide(BigDecimal.valueOf(12), CALC_CONTEXT);

        BigDecimal balance = principal;
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);
        List<Installment> installments = new ArrayList<>(months);

        for (int sequence = 1; sequence <= months; sequence++) {
            BigDecimal interestPart = scaleMoney(balance.multiply(monthlyRate, CALC_CONTEXT));
            BigDecimal principalPart;
            BigDecimal amountDue;

            if (sequence == months) {
                principalPart = scaleMoney(balance);
                amountDue = scaleMoney(principalPart.add(interestPart));
                balance = BigDecimal.ZERO;
            } else {
                amountDue = monthlyPayment;
                principalPart = scaleMoney(monthlyPayment.subtract(interestPart));
                balance = scaleMoney(balance.subtract(principalPart));
            }

            installments.add(Installment.builder()
                    .sequenceNumber(sequence)
                    .dueDate(firstDueDate.plusMonths(sequence - 1L))
                    .amountDue(amountDue)
                    .principalPart(principalPart)
                    .interestPart(interestPart)
                    .remainingBalance(scaleMoney(balance))
                    .status(InstallmentStatus.UPCOMING)
                    .attemptCount(0)
                    .build());
        }

        return installments;
    }

    private static BigDecimal requirePositive(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Le " + label + " doit être renseigné et strictement positif.");
        }
        return value;
    }

    private static int requirePositiveMonths(Integer months) {
        if (months == null || months <= 0) {
            throw new BusinessRuleException("La durée accordée doit être strictement positive.");
        }
        return months;
    }

    private static BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
