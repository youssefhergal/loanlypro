package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.service.ClientDocumentsService;
import com.projetfilrouge.loanmanagement.service.IssuedDocumentService;
import com.projetfilrouge.loanmanagement.service.RepaymentDocumentExportService;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.web.dto.response.CreditDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifGroupResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GED client — squelette Sprint 6 / EPIC-8. Voir JIRA/SPRINT-DOCUMENTS.md
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "GED client — justificatifs, crédit, exports PDF")
public class ClientDocumentsController {

    private final ClientDocumentsService clientDocumentsService;
    private final IssuedDocumentService issuedDocumentService;
    private final RepaymentDocumentExportService repaymentDocumentExportService;

    @GetMapping("/me/justificatifs")
    @Operation(summary = "Mes justificatifs groupés par dossier")
    public ResponseEntity<List<JustificatifGroupResponseDto>> getMyJustificatifs(Authentication authentication) {
        List<JustificatifGroupResponseDto> result = clientDocumentsService.getJustificatifsGroupedByApplication(
                authentication.getName()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/justificatifs/{documentId}/download")
    @Operation(summary = "Télécharger un justificatif")
    public ResponseEntity<byte[]> downloadJustificatif(
            Authentication authentication,
            @PathVariable Long documentId
    ) {
        DocumentDownload file = clientDocumentsService.downloadJustificatif(
                authentication.getName(),
                documentId
        );
        return asAttachment(file);
    }

    @GetMapping("/me/credit")
    @Operation(summary = "Documents crédit émis par LoanlyFans")
    public ResponseEntity<List<CreditDocumentResponseDto>> getMyCreditDocuments(Authentication authentication) {
        List<CreditDocumentResponseDto> result = issuedDocumentService.listCreditDocumentsForClient(
                authentication.getName()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/credit/{type}/{referenceId}/download")
    @Operation(summary = "Télécharger un document crédit (PDF)")
    public ResponseEntity<byte[]> downloadCreditDocument(
            Authentication authentication,
            @PathVariable IssuedDocumentType type,
            @PathVariable Long referenceId
    ) {
        DocumentDownload file = issuedDocumentService.downloadCreditDocument(
                authentication.getName(),
                type,
                referenceId
        );
        return asAttachment(file);
    }

    @GetMapping("/me/loans/{loanId}/schedule.pdf")
    @Operation(summary = "Exporter l'échéancier en PDF")
    public ResponseEntity<byte[]> downloadSchedulePdf(
            Authentication authentication,
            @PathVariable Long loanId
    ) {
        DocumentDownload file = repaymentDocumentExportService.exportSchedulePdf(
                authentication.getName(),
                loanId
        );
        return asAttachment(file);
    }

    @GetMapping("/me/loans/{loanId}/payments.pdf")
    @Operation(summary = "Exporter le relevé des prélèvements en PDF")
    public ResponseEntity<byte[]> downloadPaymentsPdf(
            Authentication authentication,
            @PathVariable Long loanId
    ) {
        DocumentDownload file = repaymentDocumentExportService.exportPaymentsPdf(
                authentication.getName(),
                loanId
        );
        return asAttachment(file);
    }

    private static ResponseEntity<byte[]> asAttachment(DocumentDownload file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
