package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.ChangePasswordRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.UpdateProfileRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.UserResponse;
import com.projetfilrouge.loanmanagement.web.dto.response.UpdateProfileResponse;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.projetfilrouge.loanmanagement.security.JwtService jwtService;

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Utilisateur non authentifié");
        }
        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return toUserResponse(getCurrentUserEntity());
    }

    @Transactional
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();
        String newEmail = request.getEmail();
        if (newEmail == null || newEmail.isBlank()) {
            throw new BusinessRuleException("L'email est obligatoire");
        }
        boolean unchanged = newEmail.equalsIgnoreCase(user.getEmail());
        // Ensure email uniqueness
        if (!unchanged && userRepository.existsByEmail(newEmail)) {
            throw new BusinessRuleException("Cet email est déjà utilisé");
        }
        if (!unchanged) {
            user.setEmail(newEmail);
            user = userRepository.save(user);
        }
        // Always issue a fresh JWT based on the (possibly unchanged) current email
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.getName()))
                        .toList()
        );
        String token = jwtService.generateToken(authentication);
        return UpdateProfileResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            // Utiliser une exception fonctionnelle pour éviter de retourner 401 (UNAUTHORIZED)
            // alors que l'utilisateur est authentifié mais a fourni un mot de passe actuel invalide.
            throw new BusinessRuleException("Mot de passe actuel invalide");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
