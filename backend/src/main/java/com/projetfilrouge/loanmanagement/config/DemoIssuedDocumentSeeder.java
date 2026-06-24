package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.repository.IssuedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed demo documents crédit — squelette US-6.4.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("demo")
@Order(50)
public class DemoIssuedDocumentSeeder implements CommandLineRunner {

    private final IssuedDocumentRepository issuedDocumentRepository;

    @Override
    public void run(String... args) {
        log.info("DemoIssuedDocumentSeeder — squelette (US-6.4 à implémenter, {} entrées en base).",
                issuedDocumentRepository.count());
    }
}
