package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.LoanService;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanSubmittedUpdateDto;
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
        try {
            return ResponseEntity.ok(loanService.getApplicationByReference(reference, authentication.getName()));
        } catch (RuntimeException ex) {
            if ("Accès refusé à cette demande".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            throw ex;
        }
    }

    @PutMapping("/{id}/submitted")
    @Operation(
            summary = "Mettre à jour une demande SOUMISE (SUBMITTED)",
            description = "Mise à jour réservée aux rôles ROLE_CONSEILLER et ROLE_ADMIN. " +
                    "Champs modifiables: assigned_advisor_id, approved_amount, approved_duration_months, interest_rate."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier mis à jour"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant ou statut non SUBMITTED)"),
            @ApiResponse(responseCode = "404", description = "Dossier ou conseiller introuvable")
    })
    public ResponseEntity<LoanResponseDto> updateSubmitted(
            @PathVariable Long id,
            @RequestBody LoanSubmittedUpdateDto request,
            Authentication authentication
    ) {
        try {
            LoanResponseDto response = loanService.updateSubmittedApplication(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            if ("Accès refusé à cette demande".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if ("Mise à jour impossible : seul un dossier soumis (SUBMITTED) peut être modifié.".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if ("Dossier introuvable".equals(ex.getMessage()) || "Conseiller introuvable".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw ex;
        }
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre une demande", description = "Envoie le dossier pour étude par un conseiller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier soumis"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant")
    })
    public ResponseEntity<LoanResponseDto> submit(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(loanService.submitApplication(id, authentication.getName()));
        } catch (RuntimeException ex) {
            if ("Accès refusé à cette demande".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            throw ex;
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une demande (DRAFT uniquement)",
            description = "Met à jour un dossier de prêt existant lorsque son statut est DRAFT. " +
                    "Seul l'auteur, le conseiller assigné ou un admin ayant accès au dossier peut effectuer la mise à jour.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody LoanRequestDto request,
            Authentication authentication
    ) {
        try {
            LoanResponseDto response = loanService.updateApplication(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            // Si l'exception correspond à la règle DRAFT uniquement, renvoyer 403
            if ("Mise à jour impossible : seul un dossier en brouillon (DRAFT) peut être modifié.".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // Si l'accès est refusé, renvoyer 403
            if ("Accès refusé à cette demande".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // Sinon, repropager pour laisser le gestionnaire global traiter l'erreur
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une demande",
            description = "Supprime un dossier. Règle: si l'utilisateur a le rôle ROLE_CLIENT, il ne peut supprimer que les dossiers en DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dossier supprimé"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Suppression interdite selon les règles d'accès"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            loanService.deleteApplication(id, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            // Si la règle spécifique client/DRAFT est violée, renvoyer 403
            if ("Suppression impossible : un client ne peut supprimer qu'un dossier en brouillon (DRAFT).".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // Si l'accès est refusé, renvoyer 403
            if ("Accès refusé à cette demande".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // Si dossier introuvable, renvoyer 404
            if("Dossier introuvable".equals(ex.getMessage())){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw ex;
        }
    }
}