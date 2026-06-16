package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.PaymentTransaction;
import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;
import com.projetfilrouge.loanmanagement.payment.PaymentProvider;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentTransactionRepository;
import com.projetfilrouge.loanmanagement.security.IbanVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DirectDebitExecutionService {

    public enum ExecutionOutcome {
        SUCCESS,
        FAILED,
        BLOCKED,
        SKIPPED,
        OVERDUE
    }

    public record ExecutionResult(ExecutionOutcome outcome) {
        public static ExecutionResult of(ExecutionOutcome outcome) {
            return new ExecutionResult(outcome);
        }
    }

    private final InstallmentRepository installmentRepository;
    private final LoanRepository loanRepository;
    private final DirectDebitMandateRepository mandateRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProvider paymentProvider;
    private final MandateService mandateService;
    private final IbanVaultService ibanVaultService;
    private final LoanApplicationHistoryService historyService;
    private final OverdueDetectionService overdueDetectionService;

    @Value("${app.repayment.scheduler.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.repayment.scheduler.retry-days:0,3,7}")
    private List<Integer> retryDays;

    @Transactional
    public ExecutionResult executeInstallment(Installment installment) {
        Loan loan = installment.getRepaymentPlan().getLoan();
        LoanApplication application = loan.getLoanApplication();

        if (loan.getStatus() == LoanStatus.CLOSED || installment.getStatus() == InstallmentStatus.PAID) {
            return ExecutionResult.of(ExecutionOutcome.SKIPPED);
        }

        if (!mandateService.hasActiveMandate(loan.getId()) || loan.getStatus() != LoanStatus.ACTIVE) {
            if (installment.getStatus() != InstallmentStatus.BLOCKED) {
                installment.setStatus(InstallmentStatus.BLOCKED);
                installment.setNextRetryDate(null);
                installmentRepository.save(installment);
            }
            return ExecutionResult.of(ExecutionOutcome.BLOCKED);
        }

        int attemptNumber = installment.getAttemptCount() + 1;
        if (installment.getId() != null
                && transactionRepository.existsByInstallmentIdAndAttemptNumber(installment.getId(), attemptNumber)) {
            return ExecutionResult.of(ExecutionOutcome.SKIPPED);
        }

        DirectDebitMandate mandate = mandateRepository.findByLoanIdAndStatus(loan.getId(), com.projetfilrouge.loanmanagement.entity.MandateStatus.ACTIVE)
                .orElseThrow();

        String idempotencyKey = "installment-" + installment.getId() + "-attempt-" + attemptNumber;
        Instant now = Instant.now();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .installment(installment)
                .attemptNumber(attemptNumber)
                .amount(installment.getAmountDue())
                .status(PaymentTransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .attemptedAt(now)
                .build();
        transactionRepository.save(transaction);

        PaymentProvider.PaymentResult result = paymentProvider.debit(new PaymentProvider.DebitRequest(
                mandate.getMandateReference(),
                ibanVaultService.resolve(mandate.getPaymentMethod().getIbanToken()),
                installment.getAmountDue(),
                "EUR",
                idempotencyKey
        ));

        installment.setAttemptCount(attemptNumber);

        if (result.status() == PaymentTransactionStatus.SUCCESS) {
            return handleSuccess(installment, loan, application, transaction, result, now);
        }

        return handleFailure(installment, loan, application, transaction, result, now);
    }

    private ExecutionResult handleSuccess(
            Installment installment,
            Loan loan,
            LoanApplication application,
            PaymentTransaction transaction,
            PaymentProvider.PaymentResult result,
            Instant now
    ) {
        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setExternalReference(result.externalReference());
        transaction.setSettledAt(now);
        transactionRepository.save(transaction);

        installment.setStatus(InstallmentStatus.PAID);
        installment.setNextRetryDate(null);
        installmentRepository.save(installment);

        loan.setRemainingBalance(installment.getRemainingBalance());
        loanRepository.save(loan);

        historyService.recordEvent(
                application,
                LoanApplicationEventType.PAYMENT_SUCCEEDED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of(
                        "installmentId", installment.getId(),
                        "amount", installment.getAmountDue(),
                        "externalReference", result.externalReference() != null ? result.externalReference() : ""
                )
        );

        overdueDetectionService.closeLoanIfFullyRepaid(loan, application);
        return ExecutionResult.of(ExecutionOutcome.SUCCESS);
    }

    private ExecutionResult handleFailure(
            Installment installment,
            Loan loan,
            LoanApplication application,
            PaymentTransaction transaction,
            PaymentProvider.PaymentResult result,
            Instant now
    ) {
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(result.failureReason());
        transaction.setSettledAt(now);
        transactionRepository.save(transaction);

        historyService.recordEvent(
                application,
                LoanApplicationEventType.PAYMENT_FAILED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of(
                        "installmentId", installment.getId(),
                        "amount", installment.getAmountDue(),
                        "failureReason", result.failureReason() != null ? result.failureReason() : "unknown"
                )
        );

        if (installment.getAttemptCount() >= maxAttempts) {
            installment.setStatus(InstallmentStatus.OVERDUE);
            installment.setNextRetryDate(null);
            installmentRepository.save(installment);
            overdueDetectionService.onInstallmentOverdue(installment, loan, application);
            return ExecutionResult.of(ExecutionOutcome.OVERDUE);
        }

        installment.setStatus(InstallmentStatus.FAILED);
        installment.setNextRetryDate(resolveNextRetryDate(installment));
        installmentRepository.save(installment);
        return ExecutionResult.of(ExecutionOutcome.FAILED);
    }

    private LocalDate resolveNextRetryDate(Installment installment) {
        int attemptIndex = installment.getAttemptCount();
        if (attemptIndex >= retryDays.size()) {
            return installment.getDueDate().plusDays(retryDays.get(retryDays.size() - 1));
        }
        return installment.getDueDate().plusDays(retryDays.get(attemptIndex));
    }
}
