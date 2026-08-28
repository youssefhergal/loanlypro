package com.projetfilrouge.loanmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cloud SQL instances created before UNDER_REVIEW / OFFER_PENDING may still store
 * {@code loan_applications.status} as a short VARCHAR or legacy MySQL ENUM.
 * Hibernate {@code ddl-auto: update} does not always widen those columns.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationStatusColumnMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!isMySql()) {
            return;
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE loan_applications MODIFY COLUMN status VARCHAR(32) NOT NULL"
            );
            log.info("Colonne loan_applications.status normalisée en VARCHAR(32).");
        } catch (Exception ex) {
            log.warn("Migration loan_applications.status ignorée: {}", ex.getMessage());
        }
    }

    private boolean isMySql() {
        try {
            String product = jdbcTemplate.getDataSource()
                    .getConnection()
                    .getMetaData()
                    .getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (Exception ex) {
            log.warn("Impossible de détecter le SGBD pour la migration status: {}", ex.getMessage());
            return false;
        }
    }
}
