package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.AdvisorAssignmentService;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorAssignmentResultDto;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdvisorAssignmentService advisorAssignmentService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void runAdvisorAssignment_returnsAssignmentResult() throws Exception {
        AdvisorAssignmentResultDto result = AdvisorAssignmentResultDto.builder()
                .assignedCount(2)
                .unassignedRemaining(0)
                .advisorsAvailable(3)
                .message("Affectation manuelle : 2 dossier(s) assigné(s).")
                .assignments(List.of())
                .build();

        when(advisorAssignmentService.assignUnassignedApplications(
                "admin@test.com",
                AdvisorAssignmentService.Trigger.MANUAL
        )).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/advisor-assignment/run")
                        .principal(new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCount").value(2))
                .andExpect(jsonPath("$.advisorsAvailable").value(3))
                .andExpect(jsonPath("$.message").value("Affectation manuelle : 2 dossier(s) assigné(s)."));

        verify(advisorAssignmentService).assignUnassignedApplications(
                "admin@test.com",
                AdvisorAssignmentService.Trigger.MANUAL
        );
    }
}
