package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.AdvisorAssignmentService;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorAssignmentResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Opérations de supervision administrateur")
public class AdminController {

    private final AdvisorAssignmentService advisorAssignmentService;

    @PostMapping("/advisor-assignment/run")
    @Operation(
            summary = "Affecter les dossiers non assignés",
            description = "Répartit les demandes SUBMITTED sans conseiller selon la charge de travail active."
    )
    @ApiResponse(responseCode = "200", description = "Affectation exécutée")
    public ResponseEntity<AdvisorAssignmentResultDto> runAdvisorAssignment(Authentication authentication) {
        return ResponseEntity.ok(advisorAssignmentService.assignUnassignedApplications(
                authentication.getName(),
                AdvisorAssignmentService.Trigger.MANUAL
        ));
    }
}
