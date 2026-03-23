package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.LoanService;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan", description = "Gestion des demandes de prêt")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Créer une demande", description = "Crée un nouveau dossier de prêt lié à l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié")
    })
    public ResponseEntity<LoanResponseDto> create(
            @Valid @RequestBody LoanRequestDto request,
            Authentication authentication // Spring injecte l'utilisateur connecté ici
    ) {
        // On récupère l'email (le username) de l'utilisateur authentifié via le JWT
        String userEmail = authentication.getName();
        LoanResponseDto response = loanService.createApplication(request, userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lister les demandes", description = "Récupère les dossiers de prêt. Note : Les clients ne voient que les leurs.")
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    public ResponseEntity<List<LoanResponseDto>> getAll(Authentication authentication) {
        return ResponseEntity.ok(loanService.getAllApplications(authentication.getName()));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Consulter une demande", description = "Récupère les détails d'un dossier via sa référence.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier trouvé"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier")
    })
    public ResponseEntity<LoanResponseDto> getByReference(
            @PathVariable String reference,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.getApplicationByReference(reference, authentication.getName()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre une demande", description = "Envoie le dossier pour étude par un conseiller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier soumis"),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant")
    })
    public ResponseEntity<LoanResponseDto> submit(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(loanService.submitApplication(id, authentication.getName()));
    }
}