package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.DocumentValidationStatus;
import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.service.ClientDocumentsService;
import com.projetfilrouge.loanmanagement.service.IssuedDocumentService;
import com.projetfilrouge.loanmanagement.service.RepaymentDocumentExportService;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.web.dto.response.CreditDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifGroupResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.JustificatifItemResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests controller GED client (US-6.1 / US-6.2 / US-6.3).
 */
@ExtendWith(MockitoExtension.class)
class ClientDocumentsControllerTest {

    private static final String CLIENT_EMAIL = "client@test.com";

    @Mock
    private ClientDocumentsService clientDocumentsService;

    @Mock
    private IssuedDocumentService issuedDocumentService;

    @Mock
    private RepaymentDocumentExportService repaymentDocumentExportService;

    @InjectMocks
    private ClientDocumentsController clientDocumentsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientDocumentsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyJustificatifs_returnsGroupedList() throws Exception {
        JustificatifItemResponseDto item = JustificatifItemResponseDto.builder()
                .documentId(10L)
                .documentType(LoanDocumentType.IDENTITY)
                .documentTypeLabel("Pièce d'identité")
                .fileName("carte.pdf")
                .uploadedAt(Instant.now())
                .validationStatus(DocumentValidationStatus.VALIDATED)
                .downloadable(true)
                .build();
        JustificatifGroupResponseDto group = JustificatifGroupResponseDto.builder()
                .loanApplicationId(1L)
                .loanReference("LF-DEMO-0001")
                .loanStatus("APPROVED")
                .documents(List.of(item))
                .build();

        when(clientDocumentsService.getJustificatifsGroupedByApplication(CLIENT_EMAIL))
                .thenReturn(List.of(group));

        mockMvc.perform(get("/api/v1/documents/me/justificatifs").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanReference").value("LF-DEMO-0001"))
                .andExpect(jsonPath("$[0].documents[0].documentTypeLabel").value("Pièce d'identité"))
                .andExpect(jsonPath("$[0].documents[0].validationStatus").value("VALIDATED"));
    }

    @Test
    void downloadJustificatif_returnsAttachment() throws Exception {
        when(clientDocumentsService.downloadJustificatif(eq(CLIENT_EMAIL), eq(10L)))
                .thenReturn(new DocumentDownload("carte.pdf", "application/pdf", new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/v1/documents/me/justificatifs/10/download").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carte.pdf\""))
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void getMyCreditDocuments_returnsList() throws Exception {
        CreditDocumentResponseDto dto = CreditDocumentResponseDto.builder()
                .documentType(IssuedDocumentType.LOAN_CONTRACT)
                .title("Contrat de prêt")
                .reference("LF-DEMO-0001")
                .loanApplicationId(1L)
                .available(true)
                .build();

        when(issuedDocumentService.listCreditDocumentsForClient(CLIENT_EMAIL))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/documents/me/credit").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentType").value("LOAN_CONTRACT"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void downloadSchedulePdf_returnsPdf() throws Exception {
        when(repaymentDocumentExportService.exportSchedulePdf(eq(CLIENT_EMAIL), eq(5L)))
                .thenReturn(new DocumentDownload("echeancier-LF-DEMO-0001.pdf", "application/pdf", new byte[]{9}));

        mockMvc.perform(get("/api/v1/documents/me/loans/5/schedule.pdf").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"echeancier-LF-DEMO-0001.pdf\""));
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(CLIENT_EMAIL, null, null);
    }
}
