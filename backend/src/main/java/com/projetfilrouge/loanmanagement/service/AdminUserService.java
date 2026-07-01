package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.AdminCreateUserRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.UserResponse;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String q, String role, Pageable pageable) {
        String roleName = null;
        if (role != null && !role.isBlank()) {
            role = role.trim().toUpperCase();
            if (role.equals("ADMIN")) roleName = ROLE_ADMIN;
            else if (role.equals("CONSEILLER")) roleName = ROLE_CONSEILLER;
            else if (role.equals("CLIENT")) roleName = "ROLE_CLIENT";
            else throw new BusinessRuleException("Rôle inconnu: " + role);
        }
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return userRepository.searchUsers(roleName, query, pageable)
                .map(this::toUserResponse);
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Cet email est déjà utilisé");
        }
        String reqRole = request.getRole().trim().toUpperCase();
        String roleName;
        if ("ADMIN".equals(reqRole)) roleName = ROLE_ADMIN;
        else if ("CONSEILLER".equals(reqRole)) roleName = ROLE_CONSEILLER;
        else throw new BusinessRuleException("Seuls les rôles ADMIN ou CONSEILLER peuvent être créés par cet endpoint");

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Le rôle "+roleName+" n'existe pas"));

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailVerified(true)
                .roles(Set.of(role))
                .build();
        user = userRepository.save(user);
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }
}
