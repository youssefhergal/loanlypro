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
import com.projetfilrouge.loanmanagement.web.dto.response.UserResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.VerifyEmailResponse;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.getName()))
                        .toList()
        );
        String token = jwtService.generateToken(authentication);
        return LoginResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BusinessRuleException("Cet email est déjà utilisé");
        }

        Role clientRole = roleRepository.findByName(ROLE_CLIENT)
                .orElseThrow(() -> new IllegalStateException("Le rôle ROLE_CLIENT n'existe pas en base"));

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(clientRole))
                .build();

        User savedUser = userRepository.save(user);
        emailVerificationService.issueVerificationCode(savedUser);

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .roles(savedUser.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .emailVerified(savedUser.isEmailVerified())
                .build();

        return RegisterResponse.builder()
                .user(userResponse)
                .message("Utilisateur enregistré. Un e-mail de vérification a été envoyé.")
                .build();
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(String email, String token) {
        emailVerificationService.verifyEmail(email, token);
        return VerifyEmailResponse.builder()
                .verified(true)
                .message("Adresse e-mail vérifiée avec succès.")
                .build();
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        emailVerificationService.resendVerificationEmail(email);
    }

    private static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
