package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.*;
import com.projetfilrouge.loanmanagement.service.HistoryActorLabels;
import com.projetfilrouge.loanmanagement.service.LoanApplicationHistoryService;
import com.projetfilrouge.loanmanagement.service.MandateService;
import com.projetfilrouge.loanmanagement.service.RepaymentPlanService;
import com.projetfilrouge.loanmanagement.storage.LoanDocumentStorageBackend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Jeu de données riche pour démo / production : 25 clients, 4 conseillers, 2 admins,
 * demandes variées (0–8 par client), prêts (0–4 par client), justificatifs réels,
 * historique et notifications.
 *
 * <p>Idempotent : skip si {@code client.seed.01@loanlypro.fr} existe déjà.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("seed")
@ConditionalOnProperty(name = "app.seed.bulk.enabled", havingValue = "true")
@Order(100)
public class ProdBulkDataSeeder implements CommandLineRunner {

    private static final String MARKER_CLIENT_EMAIL = "client.seed.01@loanlypro.fr";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String SEED_IBAN = "FR1420041010050500013M02606";

    private static final String[][] ADVISORS = {
            {"conseiller.seed.01@loanlypro.fr", "Marie", "Martin"},
            {"conseiller.seed.02@loanlypro.fr", "Thomas", "Leroy"},
            {"conseiller.seed.03@loanlypro.fr", "Claire", "Dupont"},
            {"conseiller.seed.04@loanlypro.fr", "Nicolas", "Petit"}
    };

    private static final String[][] ADMINS = {
            {"admin.seed.01@loanlypro.fr", "Pierre", "Admin"},
            {"admin.seed.02@loanlypro.fr", "Sophie", "Admin"}
    };

    private static final String[][] CLIENTS = {
            {"client.seed.01@loanlypro.fr", "Jean", "Dupont"},
            {"client.seed.02@loanlypro.fr", "Marie", "Bernard"},
            {"client.seed.03@loanlypro.fr", "Lucas", "Moreau"},
            {"client.seed.04@loanlypro.fr", "Emma", "Laurent"},
            {"client.seed.05@loanlypro.fr", "Hugo", "Simon"},
            {"client.seed.06@loanlypro.fr", "Léa", "Michel"},
            {"client.seed.07@loanlypro.fr", "Louis", "Lefebvre"},
            {"client.seed.08@loanlypro.fr", "Chloé", "Roux"},
            {"client.seed.09@loanlypro.fr", "Gabriel", "David"},
            {"client.seed.10@loanlypro.fr", "Manon", "Bertrand"},
            {"client.seed.11@loanlypro.fr", "Arthur", "Girard"},
            {"client.seed.12@loanlypro.fr", "Camille", "Bonnet"},
            {"client.seed.13@loanlypro.fr", "Jules", "Fontaine"},
            {"client.seed.14@loanlypro.fr", "Sarah", "Rousseau"},
            {"client.seed.15@loanlypro.fr", "Paul", "Vincent"},
            {"client.seed.16@loanlypro.fr", "Inès", "Muller"},
            {"client.seed.17@loanlypro.fr", "Raphaël", "Lefevre"},
            {"client.seed.18@loanlypro.fr", "Julie", "Mercier"},
            {"client.seed.19@loanlypro.fr", "Maxime", "Garnier"},
            {"client.seed.20@loanlypro.fr", "Anaïs", "Chevalier"},
            {"client.seed.21@loanlypro.fr", "Tom", "Francois"},
            {"client.seed.22@loanlypro.fr", "Clara", "Legrand"},
            {"client.seed.23@loanlypro.fr", "Nathan", "Gauthier"},
            {"client.seed.24@loanlypro.fr", "Zoé", "Perrin"},
            {"client.seed.25@loanlypro.fr", "Ethan", "Robin"}
    };

    private static final LoanApplicationStatus[] FILLER_STATUSES = {
            LoanApplicationStatus.DRAFT,
            LoanApplicationStatus.SUBMITTED,
            LoanApplicationStatus.UNDER_REVIEW,
            LoanApplicationStatus.OFFER_PENDING,
            LoanApplicationStatus.REJECTED,
            LoanApplicationStatus.CANCELLED
    };

    private static final LoanPurpose[] PURPOSES = LoanPurpose.values();
    private static final List<IssuedDocumentType> ISSUED_TYPES = List.of(
            IssuedDocumentType.APPLICATION_RECAP,
            IssuedDocumentType.OFFER,
            IssuedDocumentType.LOAN_CONTRACT,
            IssuedDocumentType.SEPA_MANDATE
    );

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanDocumentReviewRepository loanDocumentReviewRepository;
    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final IssuedDocumentRepository issuedDocumentRepository;
    private final RepaymentPlanService repaymentPlanService;
    private final MandateService mandateService;
    private final LoanApplicationHistoryService historyService;
    private final SeedDocumentLoader seedDocumentLoader;
    private final LoanDocumentStorageBackend documentStorageBackend;

    @Value("${app.seed.bulk.password}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(MARKER_CLIENT_EMAIL).isPresent()) {
            log.info("Bulk seed : données déjà présentes ({}), skip.", MARKER_CLIENT_EMAIL);
            return;
        }

        createRolesIfMissing();
        List<User> advisors = createStaff(ADVISORS, ROLE_CONSEILLER);
        createStaff(ADMINS, ROLE_ADMIN);

        int applicationCount = 0;
        int loanCount = 0;

        for (int clientIndex = 0; clientIndex < CLIENTS.length; clientIndex++) {
            User client = createVerifiedUser(CLIENTS[clientIndex], ROLE_CLIENT);
            User advisor = advisors.get(clientIndex % advisors.size());

            int targetLoans = clientIndex % 5;
            int totalApps = ((clientIndex * 3) + 5) % 9;
            if (totalApps < targetLoans) {
                totalApps = targetLoans;
            }

            int appSeq = 1;
            for (int loanSlot = 0; loanSlot < targetLoans; loanSlot++) {
                String reference = reference(clientIndex + 1, appSeq);
                LoanApplication application = createApplication(
                        client,
                        advisor,
                        reference,
                        LoanApplicationStatus.APPROVED,
                        clientIndex,
                        appSeq
                );
                applicationCount++;
                configureApprovedApplication(application, clientIndex, appSeq);
                loanApplicationRepository.save(application);
                attachDocuments(application, client, Instant.now().minus(90 - appSeq, ChronoUnit.DAYS));
                seedDocumentReviews(application, clientIndex + appSeq);
                recordApplicationHistory(application, client, advisor, clientIndex, appSeq);
                Loan loan = repaymentPlanService.createLoanFromApprovedApplication(application);
                configureLoan(loan, application, client, clientIndex, loanSlot);
                seedIssuedDocuments(application, loan);
                loanCount++;
                appSeq++;
            }

            while (appSeq <= totalApps) {
                LoanApplicationStatus status = FILLER_STATUSES[(clientIndex + appSeq) % FILLER_STATUSES.length];
                String reference = reference(clientIndex + 1, appSeq);
                LoanApplication application = createApplication(client, advisor, reference, status, clientIndex, appSeq);
                applicationCount++;
                configureStatusSpecificFields(application, status, clientIndex, appSeq);
                loanApplicationRepository.save(application);
                if (status != LoanApplicationStatus.DRAFT) {
                    attachDocuments(application, client, application.getSubmittedAt());
                    if (status == LoanApplicationStatus.UNDER_REVIEW || status == LoanApplicationStatus.OFFER_PENDING) {
                        seedDocumentReviews(application, clientIndex + appSeq);
                    }
                }
                recordApplicationHistory(application, client, advisor, clientIndex, appSeq);
                appSeq++;
            }
        }

        log.info(
                "Bulk seed terminé : {} clients, {} conseillers, {} admins, {} demandes, {} prêts. Mot de passe : [configuré via SEED_BULK_PASSWORD]",
                CLIENTS.length,
                ADVISORS.length,
                ADMINS.length,
                applicationCount,
                loanCount
        );
    }

    private void createRolesIfMissing() {
        for (String name : new String[]{ROLE_CLIENT, ROLE_CONSEILLER, ROLE_ADMIN}) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
            }
        }
    }

    private List<User> createStaff(String[][] staff, String roleName) {
        List<User> created = new ArrayList<>();
        for (String[] entry : staff) {
            created.add(createVerifiedUser(entry, roleName));
        }
        return created;
    }

    private User createVerifiedUser(String[] entry, String roleName) {
        return userRepository.findByEmail(entry[0]).orElseGet(() -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Rôle manquant : " + roleName));
            User user = User.builder()
                    .email(entry[0])
                    .firstName(entry[1])
                    .lastName(entry[2])
                    .passwordHash(passwordEncoder.encode(seedPassword))
                    .emailVerified(true)
                    .roles(new HashSet<>(Set.of(role)))
                    .build();
            return userRepository.save(user);
        });
    }

    private String reference(int clientNumber, int appNumber) {
        return String.format(Locale.ROOT, "LF-SEED-C%02d-A%02d", clientNumber, appNumber);
    }

    private LoanApplication createApplication(
            User client,
            User advisor,
            String reference,
            LoanApplicationStatus status,
            int clientIndex,
            int appSeq
    ) {
        BigDecimal amount = BigDecimal.valueOf(3_000 + ((clientIndex + 1) * appSeq * 437) % 17_000L)
                .setScale(2);
        int duration = 12 + ((clientIndex + appSeq) % 4) * 12;
        LoanPurpose purpose = PURPOSES[(clientIndex + appSeq) % PURPOSES.length];
        Instant now = Instant.now();

        return LoanApplication.builder()
                .reference(reference)
                .applicant(client)
                .assignedAdvisor(status == LoanApplicationStatus.DRAFT ? null : advisor)
                .status(status)
                .requestedAmount(amount)
                .requestedDurationMonths(duration)
                .title(titleFor(purpose))
                .loanPurpose(purpose)
                .purpose("Projet seed — " + purpose.name().toLowerCase(Locale.ROOT))
                .monthlyIncome(BigDecimal.valueOf(2_200 + (clientIndex % 8) * 350L).setScale(2))
                .employmentStatus(EmploymentStatus.CDI)
                .submittedAt(status == LoanApplicationStatus.DRAFT ? null : now.minus(60L + appSeq, ChronoUnit.DAYS))
                .build();
    }

    private void configureApprovedApplication(LoanApplication application, int clientIndex, int appSeq) {
        application.setApprovedAmount(application.getRequestedAmount());
        application.setApprovedDurationMonths(application.getRequestedDurationMonths());
        application.setInterestRate(BigDecimal.valueOf(3.50 + ((clientIndex + appSeq) % 5) * 0.15).setScale(2));
        application.setDecidedAt(application.getSubmittedAt().plus(10, ChronoUnit.DAYS));
        application.setOfferMessage("Offre acceptée — conditions seed.");
        application.setOfferClientAccepted(true);
    }

    private void configureStatusSpecificFields(
            LoanApplication application,
            LoanApplicationStatus status,
            int clientIndex,
            int appSeq
    ) {
        Instant submitted = application.getSubmittedAt();
        if (submitted == null) {
            return;
        }
        switch (status) {
            case UNDER_REVIEW -> application.setDecidedAt(null);
            case OFFER_PENDING -> {
                application.setApprovedAmount(application.getRequestedAmount());
                application.setApprovedDurationMonths(application.getRequestedDurationMonths());
                application.setInterestRate(BigDecimal.valueOf(3.90).setScale(2));
                application.setDecidedAt(submitted.plus(12, ChronoUnit.DAYS));
                application.setOfferMessage("Offre transmise — en attente de réponse client.");
                application.setOfferClientAccepted(false);
            }
            case REJECTED -> {
                application.setDecidedAt(submitted.plus(15, ChronoUnit.DAYS));
                application.setDecisionComment("Dossier seed refusé — capacité d'emprunt insuffisante.");
            }
            case CANCELLED -> application.setDecidedAt(submitted.plus(3, ChronoUnit.DAYS));
            default -> { }
        }
    }

    private void attachDocuments(LoanApplication application, User client, Instant uploadedAround) {
        if (!loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(application.getId()).isEmpty()) {
            return;
        }
        Instant uploadTime = uploadedAround != null ? uploadedAround.plus(1, ChronoUnit.HOURS) : Instant.now();
        for (SeedDocumentLoader.SeedDocument seedDoc : seedDocumentLoader.getAll()) {
            String storedFileName = UUID.randomUUID() + "-" + sanitize(seedDoc.originalFileName());
            var stored = documentStorageBackend.storeUpload(
                    application.getId(),
                    storedFileName,
                    seedDoc.originalFileName(),
                    seedDoc.contentType(),
                    seedDoc.content()
            );
            LoanDocument document = LoanDocument.builder()
                    .loanApplication(application)
                    .documentType(seedDoc.type())
                    .originalFileName(stored.originalFileName())
                    .storedFileName(stored.storedFileName())
                    .contentType(stored.contentType())
                    .fileSizeBytes(stored.fileSizeBytes())
                    .storagePath(stored.storagePath())
                    .build();
            loanDocumentRepository.save(document);
            historyService.recordEvent(
                    application,
                    LoanApplicationEventType.DOCUMENT_UPLOADED,
                    LoanEventActorType.CLIENT,
                    client.getEmail(),
                    client.getFirstName() + " " + client.getLastName(),
                    Map.of("documentType", seedDoc.type().name(), "fileName", seedDoc.originalFileName()),
                    uploadTime
            );
            uploadTime = uploadTime.plus(5, ChronoUnit.MINUTES);
        }
    }

    private void seedDocumentReviews(LoanApplication application, int salt) {
        LoanDocumentType[] types = LoanDocumentType.values();
        for (int i = 0; i < 5; i++) {
            LoanDocumentType type = types[i];
            if (type == LoanDocumentType.OTHER) {
                continue;
            }
            if (loanDocumentReviewRepository.findByLoanApplicationIdAndDocumentType(application.getId(), type).isPresent()) {
                continue;
            }
            LoanDocumentReviewStatus reviewStatus = (salt + i) % 7 == 0
                    ? LoanDocumentReviewStatus.REJECTED
                    : LoanDocumentReviewStatus.VALIDATED;
            loanDocumentReviewRepository.save(LoanDocumentReview.builder()
                    .loanApplication(application)
                    .documentType(type)
                    .reviewStatus(reviewStatus)
                    .reviewComment(reviewStatus == LoanDocumentReviewStatus.REJECTED
                            ? "Document illisible (seed)."
                            : null)
                    .build());
        }
    }

    private void recordApplicationHistory(
            LoanApplication application,
            User client,
            User advisor,
            int clientIndex,
            int appSeq
    ) {
        Instant base = application.getSubmittedAt() != null
                ? application.getSubmittedAt().minus(2, ChronoUnit.HOURS)
                : Instant.now().minus(5, ChronoUnit.DAYS);

        historyService.recordEvent(
                application,
                LoanApplicationEventType.APPLICATION_CREATED,
                LoanEventActorType.CLIENT,
                client.getEmail(),
                client.getFirstName() + " " + client.getLastName(),
                Map.of("reference", application.getReference()),
                base
        );

        if (application.getStatus() == LoanApplicationStatus.DRAFT) {
            return;
        }

        historyService.recordEvent(
                application,
                LoanApplicationEventType.APPLICATION_SUBMITTED,
                LoanEventActorType.CLIENT,
                client.getEmail(),
                client.getFirstName() + " " + client.getLastName(),
                Map.of("reference", application.getReference()),
                application.getSubmittedAt()
        );

        historyService.recordEvent(
                application,
                LoanApplicationEventType.ADVISOR_ASSIGNED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of("advisorEmail", advisor.getEmail()),
                application.getSubmittedAt().plus(6, ChronoUnit.HOURS)
        );

        switch (application.getStatus()) {
            case UNDER_REVIEW -> historyService.recordEvent(
                    application,
                    LoanApplicationEventType.REVIEW_STARTED,
                    LoanEventActorType.ADVISOR,
                    advisor.getEmail(),
                    advisor.getFirstName() + " " + advisor.getLastName(),
                    Map.of(),
                    application.getSubmittedAt().plus(1, ChronoUnit.DAYS)
            );
            case OFFER_PENDING -> {
                historyService.recordEvent(
                        application,
                        LoanApplicationEventType.REVIEW_STARTED,
                        LoanEventActorType.ADVISOR,
                        advisor.getEmail(),
                        advisor.getFirstName() + " " + advisor.getLastName(),
                        Map.of(),
                        application.getSubmittedAt().plus(1, ChronoUnit.DAYS)
                );
                historyService.recordEvent(
                        application,
                        LoanApplicationEventType.OFFER_PROPOSED,
                        LoanEventActorType.ADVISOR,
                        advisor.getEmail(),
                        advisor.getFirstName() + " " + advisor.getLastName(),
                        Map.of("amount", application.getApprovedAmount()),
                        application.getDecidedAt()
                );
            }
            case APPROVED -> {
                historyService.recordEvent(
                        application,
                        LoanApplicationEventType.REVIEW_STARTED,
                        LoanEventActorType.ADVISOR,
                        advisor.getEmail(),
                        advisor.getFirstName() + " " + advisor.getLastName(),
                        Map.of(),
                        application.getSubmittedAt().plus(1, ChronoUnit.DAYS)
                );
                historyService.recordEvent(
                        application,
                        LoanApplicationEventType.APPLICATION_APPROVED,
                        LoanEventActorType.ADVISOR,
                        advisor.getEmail(),
                        advisor.getFirstName() + " " + advisor.getLastName(),
                        Map.of("amount", application.getApprovedAmount()),
                        application.getDecidedAt()
                );
                historyService.recordEvent(
                        application,
                        LoanApplicationEventType.OFFER_ACCEPTED,
                        LoanEventActorType.CLIENT,
                        client.getEmail(),
                        client.getFirstName() + " " + client.getLastName(),
                        Map.of(),
                        application.getDecidedAt().plus(1, ChronoUnit.DAYS)
                );
            }
            case REJECTED -> historyService.recordEvent(
                    application,
                    LoanApplicationEventType.APPLICATION_REJECTED,
                    LoanEventActorType.ADVISOR,
                    advisor.getEmail(),
                    advisor.getFirstName() + " " + advisor.getLastName(),
                    Map.of("comment", application.getDecisionComment()),
                    application.getDecidedAt()
            );
            case CANCELLED -> historyService.recordEvent(
                    application,
                    LoanApplicationEventType.APPLICATION_CANCELLED,
                    LoanEventActorType.CLIENT,
                    client.getEmail(),
                    client.getFirstName() + " " + client.getLastName(),
                    Map.of(),
                    application.getDecidedAt()
            );
            default -> { }
        }
    }

    private void configureLoan(Loan loan, LoanApplication application, User client, int clientIndex, int loanSlot) {
        int scenario = loanSlot % 4;
        switch (scenario) {
            case 0 -> configureActiveLoan(loan, application, client);
            case 1 -> configureClosedLoan(loan, application);
            case 2 -> configureDefaultedLoan(loan, application, client);
            default -> { }
        }
    }

    private void configureActiveLoan(Loan loan, LoanApplication application, User client) {
        mandateService.registerPaymentMethodAndActivateMandate(
                loan.getId(),
                client.getEmail(),
                SEED_IBAN,
                client.getFirstName() + " " + client.getLastName()
        );
        loan = loanRepository.findById(loan.getId()).orElse(loan);
        tuneActiveInstallments(loan);
        seedPaymentTransactions(loan);
    }

    private void configureClosedLoan(Loan loan, LoanApplication application) {
        mandateService.registerPaymentMethodAndActivateMandate(
                loan.getId(),
                application.getApplicant().getEmail(),
                SEED_IBAN,
                application.getApplicant().getFirstName() + " " + application.getApplicant().getLastName()
        );
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan == null) {
            return;
        }
        List<Installment> installments = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
        for (Installment installment : installments) {
            installment.setStatus(InstallmentStatus.PAID);
            installment.setAttemptCount(1);
            installment.setNextRetryDate(null);
            installmentRepository.save(installment);
        }
        loan.setRemainingBalance(BigDecimal.ZERO.setScale(2));
        loan.setStatus(LoanStatus.CLOSED);
        loan.setActivatedAt(Instant.now().minus(400, ChronoUnit.DAYS));
        loan.setClosedAt(Instant.now().minus(20, ChronoUnit.DAYS));
        loanRepository.save(loan);
        historyService.recordEvent(
                application,
                LoanApplicationEventType.LOAN_CLOSED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of("loanId", loan.getId()),
                loan.getClosedAt()
        );
    }

    private void configureDefaultedLoan(Loan loan, LoanApplication application, User client) {
        mandateService.registerPaymentMethodAndActivateMandate(
                loan.getId(),
                client.getEmail(),
                SEED_IBAN,
                client.getFirstName() + " " + client.getLastName()
        );
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan == null) {
            return;
        }
        List<Installment> installments = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
        LocalDate today = LocalDate.now();
        BigDecimal balance = loan.getPrincipalAmount();
        for (int i = 0; i < installments.size(); i++) {
            Installment installment = installments.get(i);
            installment.setDueDate(today.minusMonths(6L - i).withDayOfMonth(15));
            if (i < 2) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setAttemptCount(1);
                balance = installment.getRemainingBalance();
            } else if (i < 5) {
                installment.setStatus(InstallmentStatus.OVERDUE);
                installment.setAttemptCount(2);
                installment.setNextRetryDate(today.plusDays(2));
            } else {
                installment.setStatus(InstallmentStatus.UPCOMING);
                installment.setAttemptCount(0);
            }
            installmentRepository.save(installment);
        }
        loan.setRemainingBalance(balance);
        loan.setStatus(LoanStatus.DEFAULTED);
        loan.setActivatedAt(Instant.now().minus(200, ChronoUnit.DAYS));
        loanRepository.save(loan);
        historyService.recordEvent(
                application,
                LoanApplicationEventType.INSTALLMENT_OVERDUE,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of("loanId", loan.getId()),
                Instant.now().minus(5, ChronoUnit.DAYS)
        );
        historyService.recordEvent(
                application,
                LoanApplicationEventType.LOAN_DEFAULTED,
                LoanEventActorType.SYSTEM,
                HistoryActorLabels.SYSTEM_EMAIL,
                HistoryActorLabels.SYSTEM_DISPLAY_NAME,
                Map.of("loanId", loan.getId()),
                Instant.now().minus(2, ChronoUnit.DAYS)
        );
    }

    private void tuneActiveInstallments(Loan loan) {
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan == null) {
            return;
        }
        List<Installment> installments = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
        if (installments.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        BigDecimal balance = loan.getPrincipalAmount();
        for (int i = 0; i < installments.size(); i++) {
            Installment installment = installments.get(i);
            installment.setDueDate(today.minusMonths(3L - i).withDayOfMonth(Math.min(today.getDayOfMonth(), 28)));
            if (i < 2) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setAttemptCount(1);
                balance = installment.getRemainingBalance();
            } else if (i == 2) {
                installment.setStatus(InstallmentStatus.FAILED);
                installment.setAttemptCount(1);
                installment.setNextRetryDate(today.plusDays(3));
            } else {
                installment.setStatus(InstallmentStatus.UPCOMING);
                installment.setAttemptCount(0);
            }
            installmentRepository.save(installment);
        }
        loan.setRemainingBalance(balance);
        loan.setStatus(LoanStatus.ACTIVE);
        if (loan.getActivatedAt() == null) {
            loan.setActivatedAt(Instant.now().minus(60, ChronoUnit.DAYS));
        }
        loanRepository.save(loan);
    }

    private void seedPaymentTransactions(Loan loan) {
        if (!paymentTransactionRepository.findByLoanIdOrderByAttemptedAtDesc(loan.getId()).isEmpty()) {
            return;
        }
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        if (plan == null) {
            return;
        }
        List<Installment> installments = installmentRepository
                .findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId());
        if (installments.isEmpty()) {
            return;
        }
        Installment paid = installments.get(0);
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .installment(paid)
                .attemptNumber(1)
                .amount(paid.getAmountDue())
                .status(PaymentTransactionStatus.SUCCESS)
                .externalReference("SEPA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .idempotencyKey("seed-success-" + loan.getId())
                .attemptedAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .settledAt(Instant.now().minus(39, ChronoUnit.DAYS))
                .build());
        if (installments.size() > 2) {
            Installment failed = installments.get(2);
            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .installment(failed)
                    .attemptNumber(1)
                    .amount(failed.getAmountDue())
                    .status(PaymentTransactionStatus.FAILED)
                    .failureReason("Prélèvement rejeté (seed)")
                    .externalReference("SEPA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                    .idempotencyKey("seed-failed-" + loan.getId())
                    .attemptedAt(Instant.now().minus(3, ChronoUnit.DAYS))
                    .build());
        }
    }

    private void seedIssuedDocuments(LoanApplication application, Loan loan) {
        for (IssuedDocumentType type : ISSUED_TYPES) {
            if (issuedDocumentRepository.existsByLoanApplication_IdAndDocumentType(application.getId(), type)) {
                continue;
            }
            issuedDocumentRepository.save(IssuedDocument.builder()
                    .loanApplication(application)
                    .loanId(loan.getId())
                    .documentType(type)
                    .fileName(issuedFileName(type, application.getReference()))
                    .build());
        }
    }

    private static String issuedFileName(IssuedDocumentType type, String reference) {
        String prefix = switch (type) {
            case APPLICATION_RECAP -> "recapitulatif";
            case OFFER -> "offre";
            case LOAN_CONTRACT -> "contrat";
            case SEPA_MANDATE -> "mandat-sepa";
        };
        return prefix + "-" + reference + ".pdf";
    }

    private static String titleFor(LoanPurpose purpose) {
        return switch (purpose) {
            case VEHICLE -> "Crédit véhicule";
            case HOME_IMPROVEMENT -> "Travaux habitat";
            case EDUCATION -> "Crédit formation";
            case GREEN -> "Projet éco-responsable";
            case SOFTWARE -> "Équipement informatique";
            case OTHER -> "Projet divers";
            case PERSONAL -> "Crédit personnel";
        };
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
