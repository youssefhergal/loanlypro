package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.security.JwtService;
import com.projetfilrouge.loanmanagement.web.dto.request.LoginRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.RegisterRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.LoginResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.RegisterResponse;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_returnsTokenAndUserWhenCredentialsAreValid() {
        User user = clientUser();
        LoginRequest request = LoginRequest.builder()
                .email("client@test.com")
                .password("password")
                .build();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(any(Authentication.class))).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("client@test.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Jean");
        assertThat(response.getUser().getRoles()).containsExactly(ROLE_CLIENT);
        verify(jwtService).generateToken(any(Authentication.class));
    }

    @Test
    void login_throwsBadCredentialsWhenEmailIsUnknown() {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@test.com")
                .password("password")
                .build();

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_throwsBadCredentialsWhenPasswordIsWrong() {
        User user = clientUser();
        LoginRequest request = LoginRequest.builder()
                .email("client@test.com")
                .password("wrong-password")
                .build();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void register_createsClientUserAndReturnsResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.com")
                .firstname("Alice")
                .lastname("Martin")
                .password("password123")
                .build();
        Role clientRole = Role.builder().id(1L).name(ROLE_CLIENT).build();
        User savedUser = User.builder()
                .id(42L)
                .email("new@test.com")
                .passwordHash("encoded-password")
                .firstName("Alice")
                .lastName("Martin")
                .roles(Set.of(clientRole))
                .build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findByName(ROLE_CLIENT)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertThat(response.getMessage()).isEqualTo("Utilisateur enregistré avec succès");
        assertThat(response.getUser().getId()).isEqualTo(42L);
        assertThat(response.getUser().getEmail()).isEqualTo("new@test.com");
        assertThat(response.getUser().getRoles()).containsExactly(ROLE_CLIENT);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("new@test.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void register_throwsBusinessRuleExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.com")
                .firstname("Bob")
                .lastname("Dupont")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cet email est déjà utilisé");

        verify(userRepository, never()).save(any());
    }

    private static User clientUser() {
        Role clientRole = Role.builder().id(1L).name(ROLE_CLIENT).build();
        return User.builder()
                .id(10L)
                .email("client@test.com")
                .passwordHash("hashed-password")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(Set.of(clientRole))
                .build();
    }
}
