package com.projetfilrouge.loanmanagement.service.documents;

import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;

/**
 * Génération PDF — US-6.2 / US-6.3.
 */
public interface LoanDocumentPdfGenerator {

    byte[] generateCreditDocument(IssuedDocumentType type, LoanApplication loanApplication);

    byte[] generateSchedulePdf(Long loanId, String borrowerEmail);

    byte[] generatePaymentsPdf(Long loanId, String borrowerEmail);
}
