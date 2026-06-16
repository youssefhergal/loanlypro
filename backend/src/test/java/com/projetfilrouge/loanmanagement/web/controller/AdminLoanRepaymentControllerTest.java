package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.RepaymentQueryService;
import com.projetfilrouge.loanmanagement.web.dto.response.RepaymentKpiDto;
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
class AdminLoanRepaymentControllerTest {

    private static final String ADMIN_EMAIL = "admin@test.com";

    @Mock
    private RepaymentQueryService repaymentQueryService;

    @InjectMocks
    private AdminLoanRepaymentController adminLoanRepaymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminLoanRepaymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getKpi_returnsRepaymentKpi() throws Exception {
        RepaymentKpiDto kpi = RepaymentKpiDto.builder()
                .activeLoansCount(24L)
                .closedLoansCount(8L)
                .totalOutstanding(new BigDecimal("385000.00"))
                .collectedThisMonth(new BigDecimal("12450.00"))
                .failureRatePercent(new BigDecimal("3.2"))
                .overdueInstallmentsCount(2L)
                .build();

        when(repaymentQueryService.getAdminKpi(ADMIN_EMAIL)).thenReturn(kpi);

        mockMvc.perform(get("/api/v1/admin/loans/kpi")
                        .principal(new UsernamePasswordAuthenticationToken(ADMIN_EMAIL, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeLoansCount").value(24))
                .andExpect(jsonPath("$.overdueInstallmentsCount").value(2));

        verify(repaymentQueryService).getAdminKpi(ADMIN_EMAIL);
    }
}
