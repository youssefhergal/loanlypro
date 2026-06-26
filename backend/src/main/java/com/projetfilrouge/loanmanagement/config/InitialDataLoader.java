package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentTransactionRepository;
import com.projetfilrouge.loanmanagement.service.RepaymentPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private static final String DEFAULT_TEST_PASSWORD = "password";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RepaymentPlanService repaymentPlanService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createRolesIfMissing();
        createTestUsersIfMissing();
        createSampleClientDataIfMissing();
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

    private     void createTestUsersIfMissing() {
        createUserIfMissing("client@test.com", "Jean", "Dupont", DEFAULT_TEST_PASSWORD, ROLE_CLIENT);
        createUserIfMissing("conseiller@test.com", "Marie", "Martin", DEFAULT_TEST_PASSWORD, ROLE_CONSEILLER);
        createUserIfMissing("admin@test.com", "Pierre", "Admin", DEFAULT_TEST_PASSWORD, ROLE_ADMIN);
        ensureTestAccountsVerified();
    }

    private void ensureTestAccountsVerified() {
        for (String email : new String[]{"client@test.com", "conseiller@test.com", "admin@test.com"}) {
            userRepository.findByEmail(email).ifPresent(user -> {
                if (!user.isEmailVerified()) {
                    user.setEmailVerified(true);
                    user.setEmailVerificationCode(null);
                    user.setEmailVerificationExpiresAt(null);
                    userRepository.save(user);
                }
            });
        }
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
                .emailVerified(true)
                .roles(Set.of(role))
                .build();
        userRepository.save(user);
        log.info("Utilisateur de test créé: {} / {}", email, password);
    }

    /**
     * Ajoute quelques données de démonstration pour client@test.com si ce client n'a pas encore de dossiers.
     */
    private void createSampleClientDataIfMissing() {
        userRepository.findByEmail("client@test.com").ifPresent(client -> {
            List<LoanApplication> existing = loanApplicationRepository.findByApplicantEmail(client.getEmail());
            boolean hasAny = !existing.isEmpty();

            // Si le client n'a aucune donnée, on crée un jeu complet (brouillon + soumis + approuvé)
            if (!hasAny) {
                // Dossier 1: brouillon
                LoanApplication draft = LoanApplication.builder()
                        .reference(generateUniqueReference())
                        .applicant(client)
                        .status(LoanApplicationStatus.DRAFT)
                        .requestedAmount(new BigDecimal("8000.00"))
                        .requestedDurationMonths(24)
                        .title("Projet personnel - Equipement")
                        .loanPurpose(LoanPurpose.PERSONAL)
                        .purpose("Achat d'équipement et dépenses diverses")
                        .monthlyIncome(new BigDecimal("2800.00"))
                        .employmentStatus(EmploymentStatus.CDI)
                        .build();
                draft = loanApplicationRepository.save(draft);

                // Dossier 2: soumis
                Instant now = Instant.now();
                LoanApplication submitted = LoanApplication.builder()
                        .reference(generateUniqueReference())
                        .applicant(client)
                        .status(LoanApplicationStatus.SUBMITTED)
                        .requestedAmount(new BigDecimal("15000.00"))
                        .requestedDurationMonths(48)
                        .title("Travaux maison - Salle de bain")
                        .loanPurpose(LoanPurpose.HOME_IMPROVEMENT)
                        .purpose("Rénovation de la salle de bain")
                        .monthlyIncome(new BigDecimal("2800.00"))
                        .employmentStatus(EmploymentStatus.CDI)
                        .submittedAt(now)
                        .build();
                submitted = loanApplicationRepository.save(submitted);

                // Dossier 3: approuvé (avec prêt et transactions)
                LoanApplication approved = LoanApplication.builder()
                        .reference(generateUniqueReference())
                        .applicant(client)
                        .status(LoanApplicationStatus.APPROVED)
                        .requestedAmount(new BigDecimal("12000.00"))
                        .requestedDurationMonths(36)
                        .title("Projet véhicule - Occasion")
                        .loanPurpose(LoanPurpose.VEHICLE)
                        .purpose("Achat véhicule d'occasion")
                        .monthlyIncome(new BigDecimal("2800.00"))
                        .employmentStatus(EmploymentStatus.CDI)
                        .approvedAmount(new BigDecimal("12000.00"))
                        .approvedDurationMonths(36)
                        .interestRate(new BigDecimal("3.50"))
                        .submittedAt(now.minusSeconds(86_400L * 10))
                        .decidedAt(now.minusSeconds(86_400L * 3))
                        .offerMessage("Offre approuvée — félicitations !")
                        .offerClientAccepted(true)
                        .build();
                approved = loanApplicationRepository.save(approved);

                // Créer immédiatement le prêt et le plan d'amortissement pour le dossier approuvé
                com.projetfilrouge.loanmanagement.entity.Loan loan = repaymentPlanService.createLoanFromApprovedApplication(approved);

                // Récupérer les échéances et créer quelques transactions de démo si aucune n'existe
                seedTransactionsIfMissing(loan.getId());

                // Documents pour le dossier soumis
                LoanDocument doc1 = LoanDocument.builder()
                        .loanApplication(submitted)
                        .documentType(LoanDocumentType.IDENTITY)
                        .originalFileName("piece_identite.pdf")
                        .displayName("Pièce d'identité")
                        .storedFileName("piece_identite.pdf")
                        .contentType("application/pdf")
                        .fileSizeBytes(120_000L)
                        .storagePath("storage/loans/" + submitted.getId() + "/piece_identite.pdf")
                        .build();
                loanDocumentRepository.save(doc1);

                LoanDocument doc2 = LoanDocument.builder()
                        .loanApplication(submitted)
                        .documentType(LoanDocumentType.PAYSLIPS)
                        .originalFileName("bulletin_paie_m05.pdf")
                        .displayName("Bulletin de paie - Mai")
                        .storedFileName("bulletin_paie_m05.pdf")
                        .contentType("application/pdf")
                        .fileSizeBytes(95_000L)
                        .storagePath("storage/loans/" + submitted.getId() + "/bulletin_paie_m05.pdf")
                        .build();
                loanDocumentRepository.save(doc2);

                log.info("Données de démonstration créées pour {}: dossiers={}, docs={}, pret+transactions=OK",
                        client.getEmail(), 3, 2);
            } else {
                // Jeu déjà présent: on s'assure d'avoir au moins 1 dossier APPROVED avec prêt et transactions
                Instant now = Instant.now();
                LoanApplication approved = existing.stream()
                        .filter(a -> a.getStatus() == LoanApplicationStatus.APPROVED)
                        .findFirst()
                        .orElseGet(() -> {
                            LoanApplication a = LoanApplication.builder()
                                    .reference(generateUniqueReference())
                                    .applicant(client)
                                    .status(LoanApplicationStatus.APPROVED)
                                    .requestedAmount(new BigDecimal("10000.00"))
                                    .requestedDurationMonths(36)
                                    .title("Prêt approuvé (backfill)")
                                    .loanPurpose(LoanPurpose.PERSONAL)
                                    .purpose("Backfill approuvé")
                                    .monthlyIncome(new BigDecimal("2800.00"))
                                    .employmentStatus(EmploymentStatus.CDI)
                                    .approvedAmount(new BigDecimal("10000.00"))
                                    .approvedDurationMonths(36)
                                    .interestRate(new BigDecimal("3.50"))
                                    .submittedAt(now.minusSeconds(86_400L * 15))
                                    .decidedAt(now.minusSeconds(86_400L * 5))
                                    .offerClientAccepted(true)
                                    .build();
                            return loanApplicationRepository.save(a);
                        });

                // S'assurer qu'un prêt existe pour cette demande APPROVED
                com.projetfilrouge.loanmanagement.entity.Loan loan = loanRepository.findByLoanApplicationId(approved.getId())
                        .orElseGet(() -> repaymentPlanService.createLoanFromApprovedApplication(approved));

                // Créer des transactions si manquantes
                seedTransactionsIfMissing(loan.getId());

                log.info("Backfill démo: prêt et transactions vérifiés/créés pour {}.", client.getEmail());
            }
        });
    }

    private void seedTransactionsIfMissing(Long loanId) {
        // Récupérer les échéances et créer quelques transactions de démo si aucune n'existe
        repaymentPlanRepository.findByLoanId(loanId).ifPresent(plan -> {
            var installments = installmentRepository.findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
            if (!installments.isEmpty()) {
                var existingTx = paymentTransactionRepository.findByLoanIdOrderByAttemptedAtDesc(loanId);
                if (existingTx.isEmpty()) {
                    // Transaction 1: succès sur la première échéance
                    var i1 = installments.get(0);
                    PaymentTransaction t1 = PaymentTransaction.builder()
                            .installment(i1)
                            .attemptNumber(1)
                            .amount(i1.getAmountDue())
                            .status(PaymentTransactionStatus.SUCCESS)
                            .failureReason(null)
                            .externalReference("SEPA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .idempotencyKey("seed-" + UUID.randomUUID())
                            .attemptedAt(Instant.now().minusSeconds(86_400L * 2))
                            .settledAt(Instant.now().minusSeconds(86_400L))
                            .build();
                    paymentTransactionRepository.save(t1);

                    // Transaction 2: échec sur la deuxième échéance
                    if (installments.size() > 1) {
                        var i2 = installments.get(1);
                        PaymentTransaction t2 = PaymentTransaction.builder()
                                .installment(i2)
                                .attemptNumber(1)
                                .amount(i2.getAmountDue())
                                .status(PaymentTransactionStatus.FAILED)
                                .failureReason("IBAN rejeté (démo)")
                                .externalReference("SEPA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .idempotencyKey("seed-" + UUID.randomUUID())
                                .attemptedAt(Instant.now().minusSeconds(43_200L))
                                .settledAt(null)
                                .build();
                        paymentTransactionRepository.save(t2);
                    }
                }
            }
        });
    }

    private String generateUniqueReference() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!loanApplicationRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        // Fallback improbable
        return "LOAN-" + System.currentTimeMillis();
    }
}
