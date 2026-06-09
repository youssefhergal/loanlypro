package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import com.projetfilrouge.loanmanagement.service.LoanDocumentStorageService;
import com.projetfilrouge.loanmanagement.service.LoanService;
import com.projetfilrouge.loanmanagement.web.dto.request.AdminApplicationListQuery;
import com.projetfilrouge.loanmanagement.web.dto.request.CancelLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanSubmittedUpdateDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ProposeOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ValidateDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdminLoanListSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentReviewResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    private static final String CLIENT_EMAIL = "client@test.com";

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(
                new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(loanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new ResourceHttpMessageConverter(),
                        new AllEncompassingFormHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @Test
    void create_returns201() throws Exception {
        when(loanService.createApplication(any(LoanRequestDto.class), eq(CLIENT_EMAIL)))
                .thenReturn(sampleLoan());

        mockMvc.perform(post("/api/v1/loan-applications")
                        .principal(clientPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoanRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reference").value("LOAN-TEST"));
    }

    @Test
    void getAll_returnsPagedLoans() throws Exception {
        when(loanService.getAllApplications(CLIENT_EMAIL, null, 0, 10))
                .thenReturn(new PageImpl<>(List.of(sampleLoan())));

        mockMvc.perform(get("/api/v1/loan-applications")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAdminList_returnsPagedLoans() throws Exception {
        when(loanService.getAdminApplications(eq("admin@test.com"), any(AdminApplicationListQuery.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLoan())));

        mockMvc.perform(get("/api/v1/loan-applications/admin")
                        .principal(principal("admin@test.com"))
                        .param("unassignedOnly", "true")
                        .param("sort", "AMOUNT_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reference").value("LOAN-TEST"));
    }

    @Test
    void getAdminSummary_returnsSummary() throws Exception {
        AdminLoanListSummaryDto summary = AdminLoanListSummaryDto.builder()
                .totalCount(5L)
                .unassignedCount(2L)
                .statusCounts(Map.of())
                .advisors(List.of())
                .build();
        when(loanService.getAdminListSummary("admin@test.com", "dupont"))
                .thenReturn(summary);

        mockMvc.perform(get("/api/v1/loan-applications/admin/summary")
                        .principal(principal("admin@test.com"))
                        .param("search", "dupont"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.unassignedCount").value(2));
    }

    @Test
    void getById_returnsLoan() throws Exception {
        when(loanService.getApplicationById(1L, CLIENT_EMAIL)).thenReturn(sampleLoan());

        mockMvc.perform(get("/api/v1/loan-applications/1")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void updateDraft_returnsUpdatedLoan() throws Exception {
        when(loanService.updateDraftApplication(eq(1L), any(LoanRequestDto.class), eq(CLIENT_EMAIL)))
                .thenReturn(sampleLoan());

        mockMvc.perform(patch("/api/v1/loan-applications/1")
                        .principal(clientPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoanRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returnsUpdatedLoan() throws Exception {
        when(loanService.updateDraftApplication(eq(1L), any(LoanRequestDto.class), eq(CLIENT_EMAIL)))
                .thenReturn(sampleLoan());

        mockMvc.perform(put("/api/v1/loan-applications/1")
                        .principal(clientPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoanRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateSubmitted_returnsUpdatedLoan() throws Exception {
        when(loanService.updateSubmittedApplication(eq(1L), any(LoanSubmittedUpdateDto.class), eq("admin@test.com")))
                .thenReturn(sampleLoan());

        mockMvc.perform(put("/api/v1/loan-applications/1/submitted")
                        .principal(principal("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAdvisorId": 20,
                                  "approvedAmount": 14000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("LOAN-TEST"));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(loanService).deleteApplication(1L, CLIENT_EMAIL);

        mockMvc.perform(delete("/api/v1/loan-applications/1")
                        .principal(clientPrincipal()))
                .andExpect(status().isNoContent());

        verify(loanService).deleteApplication(1L, CLIENT_EMAIL);
    }

    @Test
    void submit_returnsSubmittedLoan() throws Exception {
        LoanResponseDto submitted = sampleLoan();
        submitted.setStatus(LoanApplicationStatus.SUBMITTED);
        when(loanService.submitApplication(1L, CLIENT_EMAIL)).thenReturn(submitted);

        mockMvc.perform(post("/api/v1/loan-applications/1/submit")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void proposeOffer_returnsOfferPendingLoan() throws Exception {
        LoanResponseDto offerPending = sampleLoan();
        offerPending.setStatus(LoanApplicationStatus.OFFER_PENDING);
        when(loanService.proposeCounterOffer(eq(1L), any(ProposeOfferRequestDto.class), eq("conseiller@test.com")))
                .thenReturn(offerPending);

        mockMvc.perform(post("/api/v1/loan-applications/1/propose-offer")
                        .principal(principal("conseiller@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedAmount": 12000,
                                  "approvedDurationMonths": 36,
                                  "interestRate": 4.5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFER_PENDING"));
    }

    @Test
    void acceptOffer_returnsLoan() throws Exception {
        when(loanService.acceptOffer(1L, CLIENT_EMAIL)).thenReturn(sampleLoan());

        mockMvc.perform(post("/api/v1/loan-applications/1/offer/accept")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void rejectOffer_returnsLoan() throws Exception {
        when(loanService.rejectOffer(eq(1L), any(RejectOfferRequestDto.class), eq(CLIENT_EMAIL)))
                .thenReturn(sampleLoan());

        mockMvc.perform(post("/api/v1/loan-applications/1/offer/reject")
                        .principal(clientPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Trop cher"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("LOAN-TEST"));
    }

    @Test
    void approve_returnsApprovedLoan() throws Exception {
        LoanResponseDto approved = sampleLoan();
        approved.setStatus(LoanApplicationStatus.APPROVED);
        when(loanService.approveApplication(1L, "conseiller@test.com")).thenReturn(approved);

        mockMvc.perform(post("/api/v1/loan-applications/1/approve")
                        .principal(principal("conseiller@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getHistory_returnsEvents() throws Exception {
        when(loanService.getApplicationHistory(1L, CLIENT_EMAIL))
                .thenReturn(List.of(LoanHistoryEventResponseDto.builder()
                        .title("Brouillon créé")
                        .build()));

        mockMvc.perform(get("/api/v1/loan-applications/1/history")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Brouillon créé"));
    }

    @Test
    void startReview_returnsUnderReviewLoan() throws Exception {
        LoanResponseDto underReview = sampleLoan();
        underReview.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        when(loanService.startReview(1L, "conseiller@test.com")).thenReturn(underReview);

        mockMvc.perform(post("/api/v1/loan-applications/1/start-review")
                        .principal(principal("conseiller@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void rejectDocument_returnsHistoryEvent() throws Exception {
        when(loanService.rejectDocument(eq(1L), any(RejectDocumentRequestDto.class), eq("conseiller@test.com")))
                .thenReturn(LoanHistoryEventResponseDto.builder()
                        .title("Complément demandé")
                        .build());

        mockMvc.perform(post("/api/v1/loan-applications/1/documents/reject")
                        .principal(principal("conseiller@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PAYSLIPS",
                                  "comment": "Mois manquants"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Complément demandé"));
    }

    @Test
    void validateDocument_returnsHistoryEvent() throws Exception {
        when(loanService.validateDocument(eq(1L), any(ValidateDocumentRequestDto.class), eq("conseiller@test.com")))
                .thenReturn(LoanHistoryEventResponseDto.builder()
                        .title("Document validé")
                        .build());

        mockMvc.perform(post("/api/v1/loan-applications/1/documents/validate")
                        .principal(principal("conseiller@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "IDENTITY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Document validé"));
    }

    @Test
    void reject_returnsRejectedLoan() throws Exception {
        LoanResponseDto rejected = sampleLoan();
        rejected.setStatus(LoanApplicationStatus.REJECTED);
        when(loanService.rejectApplication(eq(1L), any(RejectLoanRequestDto.class), eq("conseiller@test.com")))
                .thenReturn(rejected);

        mockMvc.perform(post("/api/v1/loan-applications/1/reject")
                        .principal(principal("conseiller@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Dossier incomplet"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancel_returnsCancelledLoan() throws Exception {
        LoanResponseDto cancelled = sampleLoan();
        cancelled.setStatus(LoanApplicationStatus.CANCELLED);
        when(loanService.cancelApplication(eq(1L), any(CancelLoanRequestDto.class), eq(CLIENT_EMAIL)))
                .thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/loan-applications/1/cancel")
                        .principal(clientPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Je renonce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void uploadDocument_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "identity.pdf",
                "application/pdf",
                "pdf".getBytes()
        );
        when(loanService.uploadDocument(
                eq(1L),
                eq(LoanDocumentType.IDENTITY),
                any(),
                isNull(),
                eq(CLIENT_EMAIL)
        )).thenReturn(LoanDocumentResponseDto.builder()
                .id(10L)
                .documentType(LoanDocumentType.IDENTITY)
                .originalFileName("identity.pdf")
                .build());

        mockMvc.perform(multipart("/api/v1/loan-applications/1/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .principal(clientPrincipal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.originalFileName").value("identity.pdf"));
    }

    @Test
    void uploadComplementDocument_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extra.pdf",
                "application/pdf",
                "extra".getBytes()
        );
        when(loanService.uploadComplementDocument(
                eq(1L),
                eq(LoanDocumentType.OTHER),
                any(),
                eq("Complément"),
                eq(CLIENT_EMAIL)
        )).thenReturn(LoanDocumentResponseDto.builder()
                .id(11L)
                .displayName("Complément")
                .build());

        mockMvc.perform(multipart("/api/v1/loan-applications/1/documents/complement")
                        .file(file)
                        .param("documentType", "OTHER")
                        .param("displayName", "Complément")
                        .principal(clientPrincipal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Complément"));
    }

    @Test
    void getDocumentReviews_returnsReviews() throws Exception {
        when(loanService.getDocumentReviews(1L, CLIENT_EMAIL))
                .thenReturn(List.of(LoanDocumentReviewResponseDto.builder()
                        .documentType(LoanDocumentType.IDENTITY)
                        .status("pending_review")
                        .build()));

        mockMvc.perform(get("/api/v1/loan-applications/1/document-reviews")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("pending_review"));
    }

    @Test
    void getDocuments_returnsDocuments() throws Exception {
        when(loanService.getDocuments(1L, CLIENT_EMAIL))
                .thenReturn(List.of(LoanDocumentResponseDto.builder()
                        .id(5L)
                        .originalFileName("id.pdf")
                        .build()));

        mockMvc.perform(get("/api/v1/loan-applications/1/documents")
                        .principal(clientPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalFileName").value("id.pdf"));
    }

    @Test
    void deleteDocument_returns204() throws Exception {
        doNothing().when(loanService).deleteDocument(1L, 8L, CLIENT_EMAIL);

        mockMvc.perform(delete("/api/v1/loan-applications/1/documents/8")
                        .principal(clientPrincipal()))
                .andExpect(status().isNoContent());

        verify(loanService).deleteDocument(1L, 8L, CLIENT_EMAIL);
    }

    @Test
    void downloadDocument_returnsFileContent() throws Exception {
        when(loanService.downloadDocument(1L, 9L, "conseiller@test.com"))
                .thenReturn(new LoanDocumentStorageService.DownloadedFile(
                        "id.pdf",
                        "application/pdf",
                        new byte[] {1, 2, 3}
                ));

        mockMvc.perform(get("/api/v1/loan-applications/1/documents/9/download")
                        .principal(principal("conseiller@test.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"id.pdf\""))
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    private static UsernamePasswordAuthenticationToken clientPrincipal() {
        return principal(CLIENT_EMAIL);
    }

    private static UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }

    private static LoanResponseDto sampleLoan() {
        return LoanResponseDto.builder()
                .id(1L)
                .reference("LOAN-TEST")
                .status(LoanApplicationStatus.DRAFT)
                .title("Prêt véhicule")
                .loanPurpose(LoanPurpose.VEHICLE)
                .requestedAmount(new BigDecimal("15000"))
                .requestedDurationMonths(48)
                .build();
    }

    private static String validLoanRequestJson() {
        return """
                {
                  "title": "Prêt véhicule",
                  "loanPurpose": "VEHICLE",
                  "requestedAmount": 15000,
                  "requestedDurationMonths": 48,
                  "monthlyIncome": 2800,
                  "employmentStatus": "CDI"
                }
                """;
    }
}
