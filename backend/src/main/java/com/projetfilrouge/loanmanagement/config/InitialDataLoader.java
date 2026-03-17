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

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
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
        createTestUserIfMissing();
    }

    private void createRolesIfMissing() {
        for (String name : new String[]{ROLE_CLIENT, ROLE_CONSEILLER, ROLE_ADMIN}) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
                log.info("Rôle créé: {}", name);
            }
        }
    }

    private void createTestUserIfMissing() {
        if (userRepository.findByEmail("client@test.com").isPresent()) {
            return;
        }
        Role clientRole = roleRepository.findByName(ROLE_CLIENT).orElseThrow();
        User testUser = User.builder()
                .email("client@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .firstName("Jean")
                .lastName("Dupont")
                .roles(Set.of(clientRole))
                .build();
        userRepository.save(testUser);
        log.info("Utilisateur de test créé: client@test.com / password");
    }
}
