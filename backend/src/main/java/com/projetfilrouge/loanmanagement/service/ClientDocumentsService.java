package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifGroupResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agrégation des justificatifs client (onglet « Mes justificatifs »).
 * Réutilise {@link com.projetfilrouge.loanmanagement.entity.LoanDocument} — ne pas dupliquer l'entité.
 */
@Service
@RequiredArgsConstructor
public class ClientDocumentsService {

    private final LoanDocumentRepository loanDocumentRepository;

    @Transactional(readOnly = true)
    public List<JustificatifGroupResponseDto> getJustificatifsGroupedByApplication(String clientEmail) {
        throw new UnsupportedOperationException("US-6.1 — à implémenter");
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadJustificatif(String clientEmail, Long documentId) {
        throw new UnsupportedOperationException("US-6.1 — à implémenter");
    }
}
