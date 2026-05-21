package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final LoanApplicationRepository loanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createRolesIfMissing();
        createTestUsersIfMissing();

        // On récupère les utilisateurs pour les lier aux prêts
        User client = userRepository.findByEmail("client@test.com").orElseThrow();
        User conseiller = userRepository.findByEmail("conseiller@test.com").orElseThrow();

        createTestLoansIfMissing(client, conseiller);
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

    private void createTestLoansIfMissing(User client, User advisor) {
        if (loanRepository.count() > 0) return;

        // 1. Un brouillon (pas de conseiller assigné)
        createLoan(
                "LOAN-DRAFT-001",
                LoanApplicationStatus.DRAFT,
                new BigDecimal("5000"),
                24,
                "Achat voiture occasion",
                new BigDecimal("2500"),
                EmploymentStatus.CDI,
                client,
                null
        );

        // 2. Une demande soumise (assignée au conseiller pour étude)
        createLoan(
                "LOAN-SUBMITTED-002",
                LoanApplicationStatus.SUBMITTED,
                new BigDecimal("15000"),
                48,
                "Travaux rénovation",
                new BigDecimal("3200"),
                EmploymentStatus.CDI,
                client,
                advisor
        );

        // 3. Une demande approuvée par le conseiller
        createLoan(
                "LOAN-APPROVED-003",
                LoanApplicationStatus.APPROVED,
                new BigDecimal("2000"),
                12,
                "Besoin de trésorerie",
                new BigDecimal("1800"),
                EmploymentStatus.FREELANCE,
                client,
                advisor
        );

        log.info("Demandes de prêt de test créées avec liaisons Applicant/Advisor.");
    }

    private void createLoan(String ref, LoanApplicationStatus status, BigDecimal amount, Integer duration,
                            String purpose, BigDecimal income, EmploymentStatus employment,
                            User applicant, User advisor) {

        LoanApplication loan = LoanApplication.builder()
                .reference(ref)
                .status(status)
                .title("Demande " + ref)
                .loanPurpose(com.projetfilrouge.loanmanagement.entity.LoanPurpose.PERSONAL)
                .requestedAmount(amount)
                .requestedDurationMonths(duration)
                .purpose(purpose)
                .monthlyIncome(income)
                .employmentStatus(employment)
                .additionalIncome(java.math.BigDecimal.ZERO)
                .monthlyRent(java.math.BigDecimal.ZERO)
                .monthlyLoanPayments(java.math.BigDecimal.ZERO)
                .monthlyAlimony(java.math.BigDecimal.ZERO)
                .monthlyOtherCharges(java.math.BigDecimal.ZERO)
                .applicant(applicant) // Liaison obligatoire
                .assignedAdvisor(advisor) // Liaison optionnelle
                .createdAt(Instant.now())
                .submittedAt(status != LoanApplicationStatus.DRAFT ? Instant.now() : null)
                .build();

        if (status == LoanApplicationStatus.APPROVED) {
            loan.setApprovedAmount(amount);
            loan.setApprovedDurationMonths(duration);
            loan.setInterestRate(new BigDecimal("3.5"));
            loan.setDecidedAt(Instant.now());
            loan.setDecisionComment("Profil solide, dossier validé.");
        }

        loanRepository.save(loan);
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