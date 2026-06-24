package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.repository.IssuedDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator;
import com.projetfilrouge.loanmanagement.web.dto.response.CreditDocumentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Documents crédit émis par LoanlyFans (onglet « Mon crédit »). US-6.2
 */
@Service
@RequiredArgsConstructor
public class IssuedDocumentService {

    private final IssuedDocumentRepository issuedDocumentRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentPdfGenerator pdfGenerator;

    @Transactional(readOnly = true)
    public List<CreditDocumentResponseDto> listCreditDocumentsForClient(String clientEmail) {
        throw new UnsupportedOperationException("US-6.2 — à implémenter");
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadCreditDocument(
            String clientEmail,
            IssuedDocumentType type,
            Long referenceId
    ) {
        throw new UnsupportedOperationException("US-6.2 — à implémenter");
    }
}
