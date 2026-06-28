package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.IssuedDocument;
import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.repository.IssuedDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seed démo documents crédit (US-6.4).
 *
 * <p>Crée des entrées {@link IssuedDocument} pour le dossier démo LF-DEMO-0001 afin
 * d'afficher des dates d'émission réalistes dans l'onglet « Mon crédit ». Idempotent :
 * ré-exécutable sans doublon. Profil {@code demo}, exécuté après le seed remboursement
 * (Order 30) qui crée le dossier, le prêt et le mandat.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("demo")
@Order(50)
public class DemoIssuedDocumentSeeder implements CommandLineRunner {

    private static final String DEMO_REFERENCE = "LF-DEMO-0001";

    private static final List<IssuedDocumentType> DEMO_TYPES = List.of(
            IssuedDocumentType.APPLICATION_RECAP,
            IssuedDocumentType.OFFER,
            IssuedDocumentType.LOAN_CONTRACT,
            IssuedDocumentType.SEPA_MANDATE
    );

    private final IssuedDocumentRepository issuedDocumentRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;

    @Override
    @Transactional
    public void run(String... args) {
        LoanApplication application = loanApplicationRepository.findByReference(DEMO_REFERENCE).orElse(null);
        if (application == null) {
            log.warn("Demo issued-document seed ignoré : dossier {} introuvable.", DEMO_REFERENCE);
            return;
        }

        Loan loan = loanRepository.findByLoanApplicationId(application.getId()).orElse(null);
        Long loanId = loan != null ? loan.getId() : null;

        int created = 0;
        for (IssuedDocumentType type : DEMO_TYPES) {
            if (issuedDocumentRepository.existsByLoanApplication_IdAndDocumentType(application.getId(), type)) {
                continue;
            }
            IssuedDocument document = IssuedDocument.builder()
                    .loanApplication(application)
                    .loanId(loanId)
                    .documentType(type)
                    .fileName(fileName(type, DEMO_REFERENCE))
                    .build();
            issuedDocumentRepository.save(document);
            created++;
        }

        log.info("Demo issued-document seed : {} document(s) crédit créé(s) pour {} (total {} en base).",
                created, DEMO_REFERENCE, issuedDocumentRepository.count());
    }

    private String fileName(IssuedDocumentType type, String reference) {
        String prefix = switch (type) {
            case APPLICATION_RECAP -> "recapitulatif";
            case OFFER -> "offre";
            case LOAN_CONTRACT -> "contrat";
            case SEPA_MANDATE -> "mandat-sepa";
        };
        return prefix + "-" + reference + ".pdf";
    }
}
