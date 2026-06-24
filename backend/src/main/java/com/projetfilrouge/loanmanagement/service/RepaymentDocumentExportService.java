package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports PDF échéancier & prélèvements (onglet 3). US-6.3
 */
@Service
@RequiredArgsConstructor
public class RepaymentDocumentExportService {

    private final LoanDocumentPdfGenerator pdfGenerator;

    @Transactional(readOnly = true)
    public DocumentDownload exportSchedulePdf(String clientEmail, Long loanId) {
        throw new UnsupportedOperationException("US-6.3 — à implémenter");
    }

    @Transactional(readOnly = true)
    public DocumentDownload exportPaymentsPdf(String clientEmail, Long loanId) {
        throw new UnsupportedOperationException("US-6.3 — à implémenter");
    }
}
