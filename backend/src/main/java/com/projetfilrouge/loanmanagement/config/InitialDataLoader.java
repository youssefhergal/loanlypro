package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Données minimales au démarrage : rôles et comptes de test (client, conseiller, admin).
 * Les demandes de prêt ne sont plus seedées — à créer via l'application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class InitialDataLoader implements CommandLineRunner {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createRolesIfMissing();
        createTestUsersIfMissing();
        log.info("Données initiales : rôles et utilisateurs de test vérifiés.");
    }

    private void createRolesIfMissing() {
        for (String name : new String[]{ROLE_CLIENT, ROLE_CONSEILLER, ROLE_ADMIN}) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
                log.info("Rôle créé: {}", name);
            }
        }
    }

    private void createTestUsersIfMissing() {
        createUserIfMissing("client@test.com", "Jean", "Dupont", "password", ROLE_CLIENT);
        createUserIfMissing("conseiller@test.com", "Marie", "Martin", "password", ROLE_CONSEILLER);
        createUserIfMissing("admin@test.com", "Pierre", "Admin", "password", ROLE_ADMIN);
    }

    private void createUserIfMissing(String email, String firstName, String lastName, String password, String roleName) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .roles(Set.of(role))
                .build();
        userRepository.save(user);
        log.info("Utilisateur de test créé: {} / {}", email, password);
    }
}
