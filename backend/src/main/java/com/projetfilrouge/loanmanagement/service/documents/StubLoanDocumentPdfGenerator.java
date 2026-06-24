package com.projetfilrouge.loanmanagement.service.documents;

import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import org.springframework.stereotype.Component;

/**
 * Implémentation squelette — à remplacer (US-6.2 / US-6.3).
 */
@Component
public class StubLoanDocumentPdfGenerator implements LoanDocumentPdfGenerator {

    @Override
    public byte[] generateCreditDocument(IssuedDocumentType type, LoanApplication loanApplication) {
        throw new UnsupportedOperationException("US-6.2 — génération PDF crédit non implémentée");
    }

    @Override
    public byte[] generateSchedulePdf(Long loanId, String borrowerEmail) {
        throw new UnsupportedOperationException("US-6.3 — export PDF échéancier non implémenté");
    }

    @Override
    public byte[] generatePaymentsPdf(Long loanId, String borrowerEmail) {
        throw new UnsupportedOperationException("US-6.3 — export PDF relevé prélèvements non implémenté");
    }
}
