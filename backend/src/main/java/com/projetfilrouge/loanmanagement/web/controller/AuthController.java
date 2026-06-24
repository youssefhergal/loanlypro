package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.AuthService;
import com.projetfilrouge.loanmanagement.web.dto.request.LoginRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.RegisterRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.ResendVerificationEmailRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.VerifyEmailRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.LoginResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.RegisterResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.VerifyEmailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentification par email et mot de passe. Retourne un JWT et les infos utilisateur.")
    @ApiResponse(responseCode = "200", description = "Connexion réussie")
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Inscription",
            description = "Crée un nouvel utilisateur. Retourne les informations de l'utilisateur créé."
    )
    @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides ou email déjà utilisé")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return new ResponseEntity<>(response, org.springframework.http.HttpStatus.CREATED);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Vérifier l'e-mail", description = "Valide le code reçu par e-mail après inscription.")
    @ApiResponse(responseCode = "200", description = "E-mail vérifié")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.getEmail(), request.getToken()));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Renvoyer le code de vérification")
    @ApiResponse(responseCode = "204", description = "E-mail renvoyé")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationEmailRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.noContent().build();
    }
}
