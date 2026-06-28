package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.PaymentTransaction;
import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentTransactionRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.service.documents.DocumentFormat;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.KeyValueLine;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.PaymentRow;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.PaymentsPdfContent;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.ScheduleRow;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.SchedulePdfContent;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Exports PDF échéancier & relevé de prélèvements (onglet 3). US-6.3
 *
 * <p>Contrôle d'accès : emprunteur du prêt uniquement. Génération à la volée,
 * aucun stockage.</p>
 */
@Service
@RequiredArgsConstructor
public class RepaymentDocumentExportService {

    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final LoanDocumentPdfGenerator pdfGenerator;

    @Transactional(readOnly = true)
    public DocumentDownload exportSchedulePdf(String clientEmail, Long loanId) {
        Loan loan = requireOwnedLoan(loanId, clientEmail);
        String reference = loan.getLoanApplication().getReference();

        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de remboursement introuvable"));

        List<ScheduleRow> rows = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId()).stream()
                .map(this::toScheduleRow)
                .toList();

        List<KeyValueLine> summary = List.of(
                new KeyValueLine("Capital", DocumentFormat.euro(loan.getPrincipalAmount())),
                new KeyValueLine("Durée", DocumentFormat.months(loan.getDurationMonths())),
                new KeyValueLine("Taux annuel", DocumentFormat.percent(loan.getAnnualRate())),
                new KeyValueLine("Mensualité", DocumentFormat.euro(plan.getMonthlyPayment())),
                new KeyValueLine("Total à rembourser", DocumentFormat.euro(plan.getTotalRepayable()))
        );

        SchedulePdfContent content = new SchedulePdfContent(
                reference, borrowerName(loan.getBorrower()), Instant.now(), summary, rows);
        byte[] pdf = pdfGenerator.generateSchedule(content);
        return new DocumentDownload("echeancier-" + reference + ".pdf", "application/pdf", pdf);
    }

    @Transactional(readOnly = true)
    public DocumentDownload exportPaymentsPdf(String clientEmail, Long loanId) {
        Loan loan = requireOwnedLoan(loanId, clientEmail);
        String reference = loan.getLoanApplication().getReference();

        List<PaymentRow> rows = paymentTransactionRepository
                .findByLoanIdOrderByAttemptedAtDesc(loanId).stream()
                .map(this::toPaymentRow)
                .toList();

        PaymentsPdfContent content = new PaymentsPdfContent(
                reference, borrowerName(loan.getBorrower()), Instant.now(), rows);
        byte[] pdf = pdfGenerator.generatePayments(content);
        return new DocumentDownload("releve-prelevements-" + reference + ".pdf", "application/pdf", pdf);
    }

    /* ----------------------------------------------------------------- helpers */

    private Loan requireOwnedLoan(Long loanId, String clientEmail) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Prêt introuvable"));
        if (!loan.getBorrower().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new ForbiddenOperationException("Accès refusé à ce prêt.");
        }
        return loan;
    }

    private ScheduleRow toScheduleRow(Installment installment) {
        return new ScheduleRow(
                installment.getSequenceNumber(),
                installment.getDueDate(),
                installment.getAmountDue(),
                installment.getPrincipalPart(),
                installment.getInterestPart(),
                installment.getRemainingBalance(),
                installmentStatusLabel(installment.getStatus()));
    }

    private PaymentRow toPaymentRow(PaymentTransaction transaction) {
        Installment installment = transaction.getInstallment();
        return new PaymentRow(
                transaction.getAttemptedAt(),
                installment.getSequenceNumber(),
                transaction.getAttemptNumber(),
                transaction.getAmount(),
                paymentStatusLabel(transaction.getStatus()),
                transaction.getFailureReason());
    }

    private String borrowerName(User borrower) {
        return borrower.getFirstName() + " " + borrower.getLastName();
    }

    private String installmentStatusLabel(InstallmentStatus status) {
        return switch (status) {
            case UPCOMING -> "À venir";
            case DUE -> "Exigible";
            case PAID -> "Payée";
            case FAILED -> "Échec";
            case OVERDUE -> "En retard";
            case BLOCKED -> "Bloquée";
        };
    }

    private String paymentStatusLabel(PaymentTransactionStatus status) {
        return switch (status) {
            case PENDING -> "En attente";
            case SUCCESS -> "Réussi";
            case FAILED -> "Échec";
            case CANCELLED -> "Annulé";
        };
    }
}
