package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.MandateStatus;
import com.projetfilrouge.loanmanagement.entity.PaymentMethod;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentMethodRepository;
import com.projetfilrouge.loanmanagement.security.IbanVaultService;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MandateService {

    private final LoanRepository loanRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final DirectDebitMandateRepository mandateRepository;
    private final IbanVaultService ibanVaultService;
    private final LoanApplicationHistoryService historyService;

    @Transactional
    public DirectDebitMandate registerPaymentMethodAndActivateMandate(
            Long loanId,
            String borrowerEmail,
            String iban,
            String holderName
    ) {
        Loan loan = requireBorrowerLoan(loanId, borrowerEmail);

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessRuleException("Impossible de configurer un mandat sur un prêt clôturé.");
        }

        String normalizedIban = normalizeIban(iban);
        if (normalizedIban.length() < 15) {
            throw new BusinessRuleException("IBAN invalide.");
        }

        DirectDebitMandate existingMandate = mandateRepository.findByLoanId(loanId).orElse(null);
        PaymentMethod paymentMethod = resolvePaymentMethod(loan, existingMandate, normalizedIban, holderName);

        DirectDebitMandate mandate = existingMandate != null
                ? existingMandate
                : DirectDebitMandate.builder()
                        .loan(loan)
                        .paymentMethod(paymentMethod)
                        .mandateReference(generateMandateReference())
                        .build();

        mandate.setPaymentMethod(paymentMethod);
        mandate.setStatus(MandateStatus.ACTIVE);
        mandate.setSignedAt(Instant.now());
        mandate.setRevokedAt(null);

        if (loan.getStatus() == LoanStatus.PENDING_MANDATE) {
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setActivatedAt(Instant.now());
            loanRepository.save(loan);
        }

        DirectDebitMandate saved = mandateRepository.save(mandate);
        recordMandateEvent(
                loan,
                LoanApplicationEventType.MANDATE_ACTIVATED,
                borrowerEmail,
                Map.of(
                        "mandateReference", saved.getMandateReference(),
                        "ibanMasked", paymentMethod.getIbanMasked()
                )
        );
        return saved;
    }

    @Transactional
    public DirectDebitMandate revokeMandate(Long loanId, String borrowerEmail) {
        Loan loan = requireBorrowerLoan(loanId, borrowerEmail);

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessRuleException("Impossible de révoquer le mandat d'un prêt clôturé.");
        }

        DirectDebitMandate mandate = mandateRepository.findByLoanIdAndStatus(loanId, MandateStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Aucun mandat actif à révoquer."));

        mandate.setStatus(MandateStatus.REVOKED);
        mandate.setRevokedAt(Instant.now());
        DirectDebitMandate saved = mandateRepository.save(mandate);

        recordMandateEvent(
                loan,
                LoanApplicationEventType.MANDATE_REVOKED,
                borrowerEmail,
                Map.of("mandateReference", saved.getMandateReference())
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMandate(Long loanId) {
        return mandateRepository.findByLoanIdAndStatus(loanId, MandateStatus.ACTIVE).isPresent();
    }

    private Loan requireBorrowerLoan(Long loanId, String borrowerEmail) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Prêt introuvable"));

        if (!loan.getBorrower().getEmail().equalsIgnoreCase(borrowerEmail)) {
            throw new ForbiddenOperationException("Accès refusé à ce prêt.");
        }
        return loan;
    }

    private PaymentMethod resolvePaymentMethod(
            Loan loan,
            DirectDebitMandate existingMandate,
            String normalizedIban,
            String holderName
    ) {
        if (existingMandate != null && existingMandate.getPaymentMethod() != null) {
            PaymentMethod existing = existingMandate.getPaymentMethod();
            existing.setIbanMasked(maskIban(normalizedIban));
            existing.setIbanToken(ibanVaultService.tokenize(normalizedIban));
            existing.setHolderName(holderName.trim());
            return paymentMethodRepository.save(existing);
        }

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(loan.getBorrower())
                .ibanMasked(maskIban(normalizedIban))
                .ibanToken(ibanVaultService.tokenize(normalizedIban))
                .holderName(holderName.trim())
                .build();
        return paymentMethodRepository.save(paymentMethod);
    }

    private void recordMandateEvent(
            Loan loan,
            LoanApplicationEventType type,
            String borrowerEmail,
            Map<String, Object> payload
    ) {
        LoanApplication application = loan.getLoanApplication();
        historyService.recordEvent(
                application,
                type,
                LoanEventActorType.CLIENT,
                borrowerEmail,
                application.getApplicant().getFirstName() + " " + application.getApplicant().getLastName(),
                payload
        );
    }

    private static String generateMandateReference() {
        return "MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String normalizeIban(String iban) {
        return iban == null ? "" : iban.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private static String maskIban(String iban) {
        if (iban.length() <= 8) {
            return iban;
        }
        return iban.substring(0, 4) + " **** **** " + iban.substring(iban.length() - 4);
    }
}
