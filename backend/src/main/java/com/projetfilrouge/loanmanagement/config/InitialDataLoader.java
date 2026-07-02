package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Données minimales au démarrage (profil {@code dev}) : rôles applicatifs uniquement.
 * Jeu de données riche : profil {@code seed} + {@code SEED_BULK_ENABLED=true} ({@link ProdBulkDataSeeder}).
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

    @Override
    public void run(String... args) {
        createRolesIfMissing();
        log.info("Données initiales : rôles vérifiés. Jeu bulk : profil seed + SEED_BULK_ENABLED=true.");
    }

    private void createRolesIfMissing() {
        for (String name : new String[]{ROLE_CLIENT, ROLE_CONSEILLER, ROLE_ADMIN}) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
                log.info("Rôle créé: {}", name);
            }
        }
    }
}
