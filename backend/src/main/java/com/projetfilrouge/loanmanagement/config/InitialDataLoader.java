package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.notification.NotificationAudience;
import com.projetfilrouge.loanmanagement.notification.NotificationContent;
import com.projetfilrouge.loanmanagement.notification.NotificationContentFactory;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentReviewRepository;
import com.projetfilrouge.loanmanagement.repository.NotificationRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
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
    private static final String PROFILE_B_EMAIL = "sophie.client@test.com";
    private static final String PROFILE_B_REFERENCE = "LF-DEMO-B001";
    private static final String PROFILE_D_EMAIL = "pierre.client@test.com";
    private static final String PROFILE_D_REFERENCE = "LF-DEMO-0000";
    private static final String PROFILE_D_ARCHIVED_REFERENCE = "LF-DEMO-D002";
    private static final String ADVISOR_DEMO_OFFER_REFERENCE = "LF-DEMO-0003";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanDocumentReviewRepository loanDocumentReviewRepository;
    private final NotificationRepository notificationRepository;
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
        createProfileBClientDataIfMissing();
        createProfileDClientDataIfMissing();
        ensureAdvisorDashboardDemoDossiers();
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
        createUserIfMissing("client@test.com", "Jean", "Dupont", DEFAULT_TEST_PASSWORD, ROLE_CLIENT);
        createUserIfMissing("marie.client@test.com", "Marie", "Leroy", DEFAULT_TEST_PASSWORD, ROLE_CLIENT);
        createUserIfMissing(PROFILE_B_EMAIL, "Sophie", "Bernard", DEFAULT_TEST_PASSWORD, ROLE_CLIENT);
        createUserIfMissing(PROFILE_D_EMAIL, "Pierre", "Moreau", DEFAULT_TEST_PASSWORD, ROLE_CLIENT);
        createUserIfMissing("conseiller@test.com", "Marie", "Martin", DEFAULT_TEST_PASSWORD, ROLE_CONSEILLER);
        createUserIfMissing("admin@test.com", "Pierre", "Admin", DEFAULT_TEST_PASSWORD, ROLE_ADMIN);
        ensureTestAccountsVerified();
    }

    private void ensureTestAccountsVerified() {
        for (String email : new String[]{
                "client@test.com",
                "marie.client@test.com",
                PROFILE_B_EMAIL,
                PROFILE_D_EMAIL,
                "conseiller@test.com",
                "admin@test.com"
        }) {
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
                .roles(new HashSet<>(Set.of(role)))
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
                User advisor = userRepository.findByEmail("conseiller@test.com").orElse(null);
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
                        .assignedAdvisor(advisor)
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

    /**
     * Profil B dashboard : dossier en cours d'étude, documents refusés, aucun prêt actif.
     */
    private void createProfileBClientDataIfMissing() {
        User client = userRepository.findByEmail(PROFILE_B_EMAIL).orElse(null);
        if (client == null) {
            return;
        }

        if (loanApplicationRepository.findByReference(PROFILE_B_REFERENCE).isPresent()) {
            return;
        }

        User advisor = userRepository.findByEmail("conseiller@test.com").orElse(null);
        Instant now = Instant.now();

        LoanApplication application = LoanApplication.builder()
                .reference(PROFILE_B_REFERENCE)
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.UNDER_REVIEW)
                .requestedAmount(new BigDecimal("12000.00"))
                .requestedDurationMonths(36)
                .title("Rénovation cuisine")
                .loanPurpose(LoanPurpose.HOME_IMPROVEMENT)
                .purpose("Travaux cuisine et électroménager")
                .monthlyIncome(new BigDecimal("3200.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .submittedAt(now.minus(3, ChronoUnit.DAYS))
                .build();
        application = loanApplicationRepository.save(application);

        LoanDocument identityDoc = LoanDocument.builder()
                .loanApplication(application)
                .documentType(LoanDocumentType.IDENTITY)
                .originalFileName("cni_sophie.pdf")
                .displayName("Pièce d'identité")
                .storedFileName("cni_sophie.pdf")
                .contentType("application/pdf")
                .fileSizeBytes(118_000L)
                .storagePath("storage/loans/" + application.getId() + "/cni_sophie.pdf")
                .build();
        loanDocumentRepository.save(identityDoc);

        LoanDocument payslipDoc = LoanDocument.builder()
                .loanApplication(application)
                .documentType(LoanDocumentType.PAYSLIPS)
                .originalFileName("bulletin_paie_sophie.pdf")
                .displayName("Bulletin de paie")
                .storedFileName("bulletin_paie_sophie.pdf")
                .contentType("application/pdf")
                .fileSizeBytes(92_000L)
                .storagePath("storage/loans/" + application.getId() + "/bulletin_paie_sophie.pdf")
                .build();
        loanDocumentRepository.save(payslipDoc);

        loanDocumentReviewRepository.save(LoanDocumentReview.builder()
                .loanApplication(application)
                .documentType(LoanDocumentType.IDENTITY)
                .reviewStatus(LoanDocumentReviewStatus.REJECTED)
                .reviewComment("Document illisible — merci de téléverser une copie nette.")
                .build());

        loanDocumentReviewRepository.save(LoanDocumentReview.builder()
                .loanApplication(application)
                .documentType(LoanDocumentType.PAYSLIPS)
                .reviewStatus(LoanDocumentReviewStatus.REJECTED)
                .reviewComment("Bulletin trop ancien — merci d'envoyer le dernier mois.")
                .build());

        if (notificationRepository.countByRecipientId(client.getId()) == 0) {
            NotificationContent submitted = NotificationContentFactory.forEvent(
                    LoanApplicationEventType.APPLICATION_SUBMITTED,
                    application,
                    NotificationAudience.CLIENT
            );
            NotificationContent reviewStarted = NotificationContentFactory.forEvent(
                    LoanApplicationEventType.REVIEW_STARTED,
                    application,
                    NotificationAudience.CLIENT
            );
            notificationRepository.save(Notification.builder()
                    .recipient(client)
                    .eventType(LoanApplicationEventType.APPLICATION_SUBMITTED)
                    .title(submitted.title())
                    .message(submitted.message())
                    .referenceType("LOAN_APPLICATION")
                    .referenceId(application.getId())
                    .createdAt(now.minus(3, ChronoUnit.DAYS))
                    .build());
            notificationRepository.save(Notification.builder()
                    .recipient(client)
                    .eventType(LoanApplicationEventType.REVIEW_STARTED)
                    .title(reviewStarted.title())
                    .message(reviewStarted.message())
                    .referenceType("LOAN_APPLICATION")
                    .referenceId(application.getId())
                    .createdAt(now.minus(2, ChronoUnit.DAYS))
                    .build());
        }

        log.info(
                "Données profil B créées pour {} : dossier {} (UNDER_REVIEW), 2 docs refusés, sans prêt.",
                PROFILE_B_EMAIL,
                PROFILE_B_REFERENCE
        );
    }

    /**
     * Profil D dashboard : prêt CLOSED entièrement remboursé, demandes archivées.
     */
    private void createProfileDClientDataIfMissing() {
        User client = userRepository.findByEmail(PROFILE_D_EMAIL).orElse(null);
        if (client == null) {
            return;
        }

        if (loanApplicationRepository.findByReference(PROFILE_D_REFERENCE).isPresent()) {
            return;
        }

        User advisor = userRepository.findByEmail("conseiller@test.com").orElse(null);
        Instant now = Instant.now();

        LoanApplication closedApplication = LoanApplication.builder()
                .reference(PROFILE_D_REFERENCE)
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.APPROVED)
                .requestedAmount(new BigDecimal("10000.00"))
                .requestedDurationMonths(20)
                .title("Prêt personnel — soldé")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet personnel remboursé intégralement")
                .monthlyIncome(new BigDecimal("3400.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .approvedAmount(new BigDecimal("10000.00"))
                .approvedDurationMonths(20)
                .interestRate(new BigDecimal("3.50"))
                .submittedAt(now.minus(700, ChronoUnit.DAYS))
                .decidedAt(now.minus(680, ChronoUnit.DAYS))
                .offerClientAccepted(true)
                .build();
        closedApplication = loanApplicationRepository.save(closedApplication);

        LoanApplication rejectedApplication = LoanApplication.builder()
                .reference(PROFILE_D_ARCHIVED_REFERENCE)
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.REJECTED)
                .requestedAmount(new BigDecimal("5000.00"))
                .requestedDurationMonths(24)
                .title("Ancienne demande refusée")
                .loanPurpose(LoanPurpose.VEHICLE)
                .purpose("Véhicule — dossier archivé")
                .monthlyIncome(new BigDecimal("3400.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .submittedAt(now.minus(900, ChronoUnit.DAYS))
                .decidedAt(now.minus(880, ChronoUnit.DAYS))
                .build();
        loanApplicationRepository.save(rejectedApplication);

        Loan loan = repaymentPlanService.createLoanFromApprovedApplication(closedApplication);
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan != null) {
            List<Installment> installments = installmentRepository
                    .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
            BigDecimal zero = BigDecimal.ZERO.setScale(2);
            for (Installment installment : installments) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setAttemptCount(1);
                installment.setNextRetryDate(null);
                installmentRepository.save(installment);
            }
            loan.setRemainingBalance(zero);
            loan.setStatus(LoanStatus.CLOSED);
            loan.setActivatedAt(now.minus(670, ChronoUnit.DAYS));
            loan.setClosedAt(now.minus(30, ChronoUnit.DAYS));
            loanRepository.save(loan);
        }

        log.info(
                "Données profil D créées pour {} : prêt {} CLOSED ({} échéances), 2 demandes archivées.",
                PROFILE_D_EMAIL,
                PROFILE_D_REFERENCE,
                plan != null ? plan.getInstallmentCount() : 0
        );
    }

    /**
     * Diversifie les statuts visibles sur le dashboard conseiller (étude, offre, soumis).
     */
    private void ensureAdvisorDashboardDemoDossiers() {
        User advisor = userRepository.findByEmail("conseiller@test.com").orElse(null);
        User jean = userRepository.findByEmail("client@test.com").orElse(null);
        if (advisor == null) {
            return;
        }

        if (jean != null) {
            loanApplicationRepository.findByApplicantEmail(jean.getEmail()).stream()
                    .filter(app -> app.getStatus() == LoanApplicationStatus.SUBMITTED
                            && app.getAssignedAdvisor() == null)
                    .findFirst()
                    .ifPresent(app -> {
                        app.setAssignedAdvisor(advisor);
                        loanApplicationRepository.save(app);
                        log.info("Dossier {} assigné à {} pour la démo conseiller.", app.getReference(), advisor.getEmail());
                    });
        }

        if (loanApplicationRepository.findByReference(ADVISOR_DEMO_OFFER_REFERENCE).isPresent()) {
            return;
        }

        if (jean == null) {
            return;
        }

        Instant now = Instant.now();
        LoanApplication offerPending = LoanApplication.builder()
                .reference(ADVISOR_DEMO_OFFER_REFERENCE)
                .applicant(jean)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.OFFER_PENDING)
                .requestedAmount(new BigDecimal("8000.00"))
                .requestedDurationMonths(36)
                .title("Crédit personnel")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet personnel — offre en attente client")
                .monthlyIncome(new BigDecimal("2800.00"))
                .employmentStatus(EmploymentStatus.CDI)
                .approvedAmount(new BigDecimal("8000.00"))
                .approvedDurationMonths(36)
                .interestRate(new BigDecimal("3.90"))
                .submittedAt(now.minus(12, ChronoUnit.DAYS))
                .decidedAt(now.minus(2, ChronoUnit.DAYS))
                .offerMessage("Offre transmise — en attente de réponse du client.")
                .offerClientAccepted(false)
                .build();
        loanApplicationRepository.save(offerPending);
        log.info("Dossier démo {} (OFFER_PENDING) créé pour le dashboard conseiller.", ADVISOR_DEMO_OFFER_REFERENCE);
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
