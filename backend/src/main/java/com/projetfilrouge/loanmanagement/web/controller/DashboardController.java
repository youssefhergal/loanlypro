package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.DashboardService;
import com.projetfilrouge.loanmanagement.web.dto.response.AdminLoanListSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorDashboardResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Données agrégées pour les tableaux de bord (client, conseiller, administrateur)")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    @Operation(summary = "Mon dashboard", description = "Récupère les transactions, demandes et documents de l'utilisateur connecté")
    public ResponseEntity<DashboardResponse> me() {
        return ResponseEntity.ok(dashboardService.getMyDashboard());
    }

    @GetMapping("/advisor")
    @Operation(summary = "Dashboard conseiller", description = "Récupère un aperçu des prêts suivis par le conseiller (top 5 + total).")
    public ResponseEntity<AdvisorDashboardResponse> advisor() {
        return ResponseEntity.ok(dashboardService.getAdvisorDashboard());
    }

    @GetMapping("/admin")
    @Operation(summary = "Dashboard administrateur", description = "Récupère une synthèse globale des demandes (compteurs par statut, non affectées, conseillers).")
    public ResponseEntity<AdminLoanListSummaryDto> admin(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dashboardService.getAdminDashboard(search));
    }
}
