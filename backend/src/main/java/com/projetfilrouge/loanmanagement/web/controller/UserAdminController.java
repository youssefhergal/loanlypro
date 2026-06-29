package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.AdminUserService;
import com.projetfilrouge.loanmanagement.web.dto.request.AdminCreateUserRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Utilisateurs", description = "Gestion des comptes utilisateurs par un administrateur")
public class UserAdminController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les utilisateurs", description = "Liste paginée des utilisateurs avec filtres optionnels par rôle et recherche texte.")
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "role", required = false) String role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminUserService.listUsers(q, role, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un utilisateur (conseiller/admin)")
    @ApiResponse(responseCode = "201", description = "Utilisateur créé")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        UserResponse created = adminUserService.createUser(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
