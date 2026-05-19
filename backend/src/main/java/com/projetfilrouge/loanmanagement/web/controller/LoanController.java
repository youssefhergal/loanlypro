package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.service.LoanService;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-applications")
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
    public ResponseEntity<Page<LoanResponseDto>> getAll(
            Authentication authentication,
            @RequestParam(required = false) LoanApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(loanService.getAllApplications(authentication.getName(), status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une demande", description = "Récupère les détails d'un dossier via son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier trouvé"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier")
    })
    public ResponseEntity<LoanResponseDto> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.getApplicationById(id, authentication.getName()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Mettre à jour un brouillon", description = "Modifie les informations d'une demande en statut DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanResponseDto> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody LoanRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.updateDraftApplication(id, request, authentication.getName()));
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

    @PostMapping("/{id}/documents")
    @Operation(summary = "Ajouter un document", description = "Upload d'un document pour une demande en brouillon.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploadé"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanDocumentResponseDto> uploadDocument(
            @PathVariable Long id,
            @RequestParam("documentType") LoanDocumentType documentType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        LoanDocumentResponseDto response = loanService.uploadDocument(id, documentType, file, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Lister les documents", description = "Récupère les documents associés à une demande.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<List<LoanDocumentResponseDto>> getDocuments(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.getDocuments(id, authentication.getName()));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Supprimer un document", description = "Supprime un document tant que le dossier est en brouillon.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier ou document introuvable")
    })
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        loanService.deleteDocument(id, documentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @Operation(summary = "Télécharger un document", description = "Télécharge un document associé à une demande accessible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document téléchargé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce dossier"),
            @ApiResponse(responseCode = "404", description = "Dossier ou document introuvable")
    })
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        LoanService.DownloadedLoanDocument file = loanService.downloadDocument(id, documentId, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}