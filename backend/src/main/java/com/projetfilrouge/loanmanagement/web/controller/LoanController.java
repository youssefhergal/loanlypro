package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.service.LoanService;
import com.projetfilrouge.loanmanagement.web.dto.request.AdminLoanListSort;
import com.projetfilrouge.loanmanagement.web.dto.request.CancelLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanSubmittedUpdateDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ProposeOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ValidateDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdminLoanListSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentReviewResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
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
            Authentication authentication
    ) {
        LoanResponseDto response = loanService.createApplication(request, authentication.getName());
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

    @GetMapping("/admin")
    @Operation(
            summary = "Lister les demandes (admin)",
            description = "Liste paginée avec filtres pour la supervision admin."
    )
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    public ResponseEntity<Page<LoanResponseDto>> getAdminList(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long advisorId,
            @RequestParam(defaultValue = "false") boolean unassignedOnly,
            @RequestParam(required = false) LoanApplicationStatus status,
            @RequestParam(defaultValue = "UPDATED_DESC") AdminLoanListSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(loanService.getAdminApplications(
                authentication.getName(),
                search,
                advisorId,
                unassignedOnly,
                status,
                sort,
                page,
                size
        ));
    }

    @GetMapping("/admin/summary")
    @Operation(
            summary = "Synthèse liste admin",
            description = "Compteurs par statut, non affectés et conseillers pour la liste admin."
    )
    @ApiResponse(responseCode = "200", description = "Synthèse récupérée avec succès")
    public ResponseEntity<AdminLoanListSummaryDto> getAdminSummary(
            Authentication authentication,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(loanService.getAdminListSummary(authentication.getName(), search));
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

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une demande (DRAFT uniquement)",
            description = "Alias PUT de la mise à jour brouillon.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody LoanRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.updateDraftApplication(id, request, authentication.getName()));
    }

    @PutMapping("/{id}/submitted")
    @Operation(
            summary = "Mettre à jour une demande SOUMISE (SUBMITTED)",
            description = "Mise à jour réservée aux rôles ROLE_CONSEILLER et ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier mis à jour"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Dossier ou conseiller introuvable")
    })
    public ResponseEntity<LoanResponseDto> updateSubmitted(
            @PathVariable Long id,
            @RequestBody LoanSubmittedUpdateDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.updateSubmittedApplication(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une demande",
            description = "Un client ne peut supprimer qu'un dossier en DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dossier supprimé"),
            @ApiResponse(responseCode = "403", description = "Suppression interdite"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        loanService.deleteApplication(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre une demande", description = "Envoie le dossier pour étude par un conseiller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier soumis"),
            @ApiResponse(responseCode = "403", description = "Accès refusé ou règles métier"),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant")
    })
    public ResponseEntity<LoanResponseDto> submit(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(loanService.submitApplication(id, authentication.getName()));
    }

    @PostMapping("/{id}/propose-offer")
    @Operation(summary = "Proposer une contre-offre", description = "Conseiller : envoie une offre ajustée en attente d'acceptation client.")
    public ResponseEntity<LoanResponseDto> proposeOffer(
            @PathVariable Long id,
            @Valid @RequestBody ProposeOfferRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.proposeCounterOffer(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/offer/accept")
    @Operation(summary = "Accepter la contre-offre", description = "Client : accepte la proposition du conseiller.")
    public ResponseEntity<LoanResponseDto> acceptOffer(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.acceptOffer(id, authentication.getName()));
    }

    @PostMapping("/{id}/offer/reject")
    @Operation(summary = "Refuser la contre-offre", description = "Client : refuse la proposition du conseiller.")
    public ResponseEntity<LoanResponseDto> rejectOffer(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid RejectOfferRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.rejectOffer(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approuver une demande", description = "Action réservée au rôle ROLE_CONSEILLER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier approuvé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanResponseDto> approve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.approveApplication(id, authentication.getName()));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historique de la demande", description = "Journal chronologique des événements du dossier.")
    public ResponseEntity<List<LoanHistoryEventResponseDto>> getHistory(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.getApplicationHistory(id, authentication.getName()));
    }

    @PostMapping("/{id}/start-review")
    @Operation(summary = "Démarrer l'analyse", description = "Passe le dossier en UNDER_REVIEW et enregistre un événement.")
    public ResponseEntity<LoanResponseDto> startReview(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.startReview(id, authentication.getName()));
    }

    @PostMapping("/{id}/documents/reject")
    @Operation(summary = "Rejeter un document", description = "Conseiller : demande de complément sur un type de pièce.")
    public ResponseEntity<LoanHistoryEventResponseDto> rejectDocument(
            @PathVariable Long id,
            @Valid @RequestBody RejectDocumentRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.rejectDocument(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/documents/validate")
    @Operation(summary = "Valider un document", description = "Conseiller : valide une pièce justificative.")
    public ResponseEntity<LoanHistoryEventResponseDto> validateDocument(
            @PathVariable Long id,
            @Valid @RequestBody ValidateDocumentRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.validateDocument(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeter une demande", description = "Action réservée au rôle ROLE_CONSEILLER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier rejeté"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanResponseDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectLoanRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.rejectApplication(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Annuler une demande",
            description = "Le demandeur peut annuler un dossier SUBMITTED ou UNDER_REVIEW."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande annulée"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanResponseDto> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid CancelLoanRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.cancelApplication(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/documents")
    @Operation(summary = "Ajouter un document", description = "Upload d'un document pour une demande en brouillon.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploadé"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    })
    public ResponseEntity<LoanDocumentResponseDto> uploadDocument(
            @PathVariable Long id,
            @RequestParam("documentType") LoanDocumentType documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName,
            Authentication authentication
    ) {
        LoanDocumentResponseDto response = loanService.uploadDocument(
                id, documentType, file, displayName, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/documents/complement")
    @Operation(summary = "Déposer un complément documentaire", description = "Client : remplace ou ajoute une pièce lorsque le dossier est en analyse (UNDER_REVIEW).")
    public ResponseEntity<LoanDocumentResponseDto> uploadComplementDocument(
            @PathVariable Long id,
            @RequestParam("documentType") LoanDocumentType documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName,
            Authentication authentication
    ) {
        LoanDocumentResponseDto response = loanService.uploadComplementDocument(
                id, documentType, file, displayName, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/document-reviews")
    @Operation(summary = "Statuts de revue des pièces", description = "Source de vérité pour la validation conseiller par type de document.")
    public ResponseEntity<List<LoanDocumentReviewResponseDto>> getDocumentReviews(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.getDocumentReviews(id, authentication.getName()));
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Lister les documents", description = "Récupère les documents associés à une demande.")
    public ResponseEntity<List<LoanDocumentResponseDto>> getDocuments(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.getDocuments(id, authentication.getName()));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Supprimer un document", description = "Supprime un document tant que le dossier est en brouillon.")
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
