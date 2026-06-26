package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.DashboardService;
import com.projetfilrouge.loanmanagement.web.dto.response.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Données agrégées pour le tableau de bord client")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    @Operation(summary = "Mon dashboard", description = "Récupère les transactions, demandes et documents de l'utilisateur connecté")
    public ResponseEntity<DashboardResponse> me() {
        return ResponseEntity.ok(dashboardService.getMyDashboard());
    }
}
