package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.ClientDocumentsService;
import com.projetfilrouge.loanmanagement.service.IssuedDocumentService;
import com.projetfilrouge.loanmanagement.service.RepaymentDocumentExportService;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Squelette tests US-6.1 — activer après implémentation.
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
    void getMyJustificatifs_notImplemented_returns501() throws Exception {
        when(clientDocumentsService.getJustificatifsGroupedByApplication(anyString()))
                .thenThrow(new UnsupportedOperationException("US-6.1 — ClientDocumentsService non implémenté"));

        mockMvc.perform(get("/api/v1/documents/me/justificatifs")
                        .principal(new UsernamePasswordAuthenticationToken(CLIENT_EMAIL, null, null)))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
    }

    @Test
    @Disabled("US-6.1 — à activer après implémentation ClientDocumentsService")
    void getMyJustificatifs_returnsGroupedList() {
        // TODO
    }
}
