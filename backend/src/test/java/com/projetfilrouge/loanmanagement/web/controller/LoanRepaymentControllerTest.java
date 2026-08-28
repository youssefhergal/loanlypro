package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.MandateService;
import com.projetfilrouge.loanmanagement.service.RepaymentQueryService;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDetailDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanSummaryDto;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentControllerTest {

    private static final String CLIENT_EMAIL = "client@test.com";

    @Mock
    private MandateService mandateService;

    @Mock
    private RepaymentQueryService repaymentQueryService;

    @InjectMocks
    private LoanRepaymentController loanRepaymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loanRepaymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyLoans_returnsClientLoans() throws Exception {
        LoanSummaryDto summary = LoanSummaryDto.builder()
                .id(1L)
                .loanApplicationId(42L)
                .reference("LF-2026-0042")
                .status("ACTIVE")
                .principalAmount(new BigDecimal("15000"))
                .remainingBalance(new BigDecimal("12450"))
                .monthlyPayment(new BigDecimal("337.68"))
                .mandateStatus("ACTIVE")
                .borrowerName("Jean Dupont")
                .overdueInstallmentsCount(0L)
                .build();

        when(repaymentQueryService.getMyLoans(CLIENT_EMAIL)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/loans/me")
                        .principal(new UsernamePasswordAuthenticationToken(CLIENT_EMAIL, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reference").value("LF-2026-0042"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(repaymentQueryService).getMyLoans(CLIENT_EMAIL);
    }

    @Test
    void getLoanDetail_returnsLoanDetail() throws Exception {
        LoanDetailDto detail = LoanDetailDto.builder()
                .id(1L)
                .loanApplicationId(42L)
                .reference("LF-2026-0042")
                .status("ACTIVE")
                .principalAmount(new BigDecimal("15000"))
                .remainingBalance(new BigDecimal("12450"))
                .monthlyPayment(new BigDecimal("337.68"))
                .paidInstallmentsCount(5L)
                .overdueInstallmentsCount(0L)
                .mandateStatus("ACTIVE")
                .build();

        when(repaymentQueryService.getLoanDetailForBorrower(1L, CLIENT_EMAIL)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/loans/1")
                        .principal(new UsernamePasswordAuthenticationToken(CLIENT_EMAIL, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.paidInstallmentsCount").value(5));

        verify(repaymentQueryService).getLoanDetailForBorrower(1L, CLIENT_EMAIL);
    }
}
