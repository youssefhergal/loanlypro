package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OverdueDetectionService {

    private final InstallmentRepository installmentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final LoanApplicationHistoryService historyService;

    @Value("${app.repayment.scheduler.overdue-threshold:2}")
    private int overdueThreshold;

    @Transactional
    public void onInstallmentOverdue(Installment installment, Loan loan, LoanApplication application) {
        historyService.recordEvent(
                application,
                LoanApplicationEventType.INSTALLMENT_OVERDUE,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of(
                        "installmentId", installment.getId(),
                        "sequenceNumber", installment.getSequenceNumber(),
                        "amount", installment.getAmountDue(),
                        "dueDate", installment.getDueDate().toString()
                )
        );
        markLoanDefaultedIfNeeded(loan, application);
    }

    @Transactional
    public void markLoanDefaultedIfNeeded(Loan loan, LoanApplication application) {
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            return;
        }
        long overdueCount = installmentRepository.countByLoanIdAndStatus(loan.getId(), InstallmentStatus.OVERDUE);
        if (overdueCount < overdueThreshold) {
            return;
        }
        loan.setStatus(LoanStatus.DEFAULTED);
        loanRepository.save(loan);
        historyService.recordEvent(
                application,
                LoanApplicationEventType.LOAN_DEFAULTED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of(
                        "loanId", loan.getId(),
                        "overdueInstallmentsCount", overdueCount
                )
        );
    }

    @Transactional
    public void closeLoanIfFullyRepaid(Loan loan, LoanApplication application) {
        if (loan.getStatus() == LoanStatus.CLOSED) {
            return;
        }
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan == null) {
            return;
        }
        List<Installment> installments = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
        boolean allPaid = installments.stream()
                .allMatch(installment -> installment.getStatus() == InstallmentStatus.PAID);
        if (!allPaid) {
            return;
        }
        loan.setStatus(LoanStatus.CLOSED);
        loan.setClosedAt(Instant.now());
        loan.setRemainingBalance(BigDecimal.ZERO);
        loanRepository.save(loan);
        historyService.recordEvent(
                application,
                LoanApplicationEventType.LOAN_CLOSED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of(
                        "loanId", loan.getId(),
                        "installmentCount", installments.size()
                )
        );
    }
}
