package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.security.JwtService;
import com.projetfilrouge.loanmanagement.service.AuthService;
import com.projetfilrouge.loanmanagement.web.dto.request.LoginRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.RegisterRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.LoginResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.RegisterResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.UserResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.VerifyEmailResponse;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void login_returns200WithTokenAndUser() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("jwt-token")
                .user(UserResponse.builder()
                        .id(1L)
                        .email("client@test.com")
                        .firstName("Jean")
                        .lastName("Dupont")
                        .roles(List.of("ROLE_CLIENT"))
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@test.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("client@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("Jean"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_CLIENT"));
    }

    @Test
    void login_returns401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Identifiants invalides"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@test.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification invalide"));
    }

    @Test
    void login_returns400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Données invalides"));
    }

    @Test
    void register_returns201WithCreatedUser() throws Exception {
        RegisterResponse response = RegisterResponse.builder()
                .message("Utilisateur enregistré. Un e-mail de vérification a été envoyé.")
                .user(UserResponse.builder()
                        .id(42L)
                        .email("new@test.com")
                        .firstName("Alice")
                        .lastName("Martin")
                        .roles(List.of("ROLE_CLIENT"))
                        .emailVerified(false)
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@test.com",
                                  "firstname": "Alice",
                                  "lastname": "Martin",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Utilisateur enregistré. Un e-mail de vérification a été envoyé."))
                .andExpect(jsonPath("$.user.id").value(42))
                .andExpect(jsonPath("$.user.email").value("new@test.com"));
    }

    @Test
    void verifyEmail_returns200WhenCodeIsValid() throws Exception {
        when(authService.verifyEmail("new@test.com", "000000"))
                .thenReturn(VerifyEmailResponse.builder()
                        .verified(true)
                        .message("Adresse e-mail vérifiée avec succès.")
                        .build());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@test.com",
                                  "token": "000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.message").value("Adresse e-mail vérifiée avec succès."));
    }

    @Test
    void resendVerification_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@test.com"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void register_returns400WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessRuleException("Cet email est déjà utilisé"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@test.com",
                                  "firstname": "Bob",
                                  "lastname": "Dupont",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
                .andExpect(jsonPath("$.message").value("Cet email est déjà utilisé"));
    }

    @Test
    void register_returns400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@test.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
