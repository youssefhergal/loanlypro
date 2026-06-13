package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.ProfilService;
import com.projetfilrouge.loanmanagement.web.dto.request.ChangePasswordRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.UpdateProfileRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.UserResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.UpdateProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profil", description = "Gestion du profil utilisateur")
public class ProfilController {

    private final ProfilService profilService;

    @GetMapping("/me")
    @Operation(summary = "Mon profil", description = "Récupère les informations du profil de l'utilisateur connecté")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(profilService.getCurrentUser());
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour l'email", description = "Met à jour uniquement l'email de l'utilisateur connecté")
    @ApiResponse(responseCode = "200", description = "Profil mis à jour")
    public ResponseEntity<UpdateProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profilService.updateProfile(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changer le mot de passe", description = "Change le mot de passe de l'utilisateur connecté")
    @ApiResponse(responseCode = "204", description = "Mot de passe changé")
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "401", description = "Non autorisé")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profilService.changePassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
