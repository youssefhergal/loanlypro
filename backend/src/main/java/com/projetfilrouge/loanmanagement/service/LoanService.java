package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanSubmittedUpdateDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final int MAX_REFERENCE_GENERATION_ATTEMPTS = 10;

    private final LoanApplicationRepository loanRepository;
    private final UserRepository userRepository;

    @Transactional
    public LoanResponseDto createApplication(LoanRequestDto request, String currentUserEmail) {
        User applicant = getRequiredUser(currentUserEmail);

        LoanApplication loanApplication = LoanApplication.builder()
                .reference(generateUniqueReference())
                .applicant(applicant)
                .status(LoanApplicationStatus.DRAFT)
                .requestedAmount(request.getRequestedAmount())
                .requestedDurationMonths(request.getRequestedDurationMonths())
                .purpose(request.getPurpose())
                .monthlyIncome(request.getMonthlyIncome())
                .employmentStatus(request.getEmploymentStatus())
                .build();

        return mapToResponseDto(loanRepository.save(loanApplication));
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDto> getAllApplications(String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        List<LoanApplication> loans;

        if (hasRole(currentUser, ROLE_ADMIN)) {
            loans = loanRepository.findAll();
        } else if (hasRole(currentUser, ROLE_CONSEILLER)) {
            loans = loanRepository.findByAssignedAdvisorId(currentUser.getId());
        } else {
            loans = loanRepository.findByApplicantEmail(currentUserEmail);
        }

        return loans.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanResponseDto getApplicationByReference(String reference, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Demande de prêt introuvable avec la référence : " + reference));
        ensureCanAccessLoan(loan, currentUser);
        return mapToResponseDto(loan);
    }

    @Transactional
    public LoanResponseDto submitApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanSubmit(loan);

        loan.setStatus(LoanApplicationStatus.SUBMITTED);
        if (loan.getSubmittedAt() == null) {
            loan.setSubmittedAt(Instant.now());
        }

        return mapToResponseDto(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponseDto updateApplication(Long id, LoanRequestDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanUpdate(loan);

        // Mettre à jour uniquement les champs éditables par l'utilisateur sur un brouillon
        loan.setRequestedAmount(request.getRequestedAmount());
        loan.setRequestedDurationMonths(request.getRequestedDurationMonths());
        loan.setPurpose(request.getPurpose());
        loan.setMonthlyIncome(request.getMonthlyIncome());
        loan.setEmploymentStatus(request.getEmploymentStatus());

        return mapToResponseDto(loanRepository.save(loan));
    }

    @Transactional
    public void deleteApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        // Vérifie l'accès (admin, conseiller assigné ou demandeur)
        ensureCanAccessLoan(loan, currentUser);

        // Règle spécifique: un client ne peut supprimer qu'un dossier en DRAFT
        if (hasRole(currentUser, ROLE_CLIENT) && loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new RuntimeException(
                    "Suppression impossible : un client ne peut supprimer qu'un dossier en brouillon (DRAFT)."
            );
        }

        loanRepository.delete(loan);
    }

    @Transactional
    public LoanResponseDto updateSubmittedApplication(Long id, LoanSubmittedUpdateDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        // Autorisé uniquement aux conseillers et admins, indépendamment de l'appartenance au dossier
        if (!(hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_CONSEILLER))) {
            throw new RuntimeException("Accès refusé à cette demande");
        }

        // L'opération ne s'applique que sur un dossier soumis
        if (loan.getStatus() != LoanApplicationStatus.SUBMITTED) {
            throw new RuntimeException("Mise à jour impossible : seul un dossier soumis (SUBMITTED) peut être modifié.");
        }

        // Mettre à jour les champs autorisés si fournis
        if (request.getAssignedAdvisorId() != null) {
            User advisor = userRepository.findById(request.getAssignedAdvisorId())
                    .orElseThrow(() -> new RuntimeException("Conseiller introuvable"));
            loan.setAssignedAdvisor(advisor);
        }
        if (request.getApprovedAmount() != null) {
            loan.setApprovedAmount(request.getApprovedAmount());
        }
        if (request.getApprovedDurationMonths() != null) {
            loan.setApprovedDurationMonths(request.getApprovedDurationMonths());
        }
        if (request.getInterestRate() != null) {
            loan.setInterestRate(request.getInterestRate());
        }

        return mapToResponseDto(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponseDto approveApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        // Autorisé uniquement aux conseillers
        if (!hasRole(currentUser, ROLE_CONSEILLER)) {
            throw new RuntimeException("Accès refusé à cette demande");
        }

        // Statut éligible: SUBMITTED ou UNDER_REVIEW
        if (!(loan.getStatus() == LoanApplicationStatus.SUBMITTED || loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW)) {
            throw new RuntimeException("Approbation impossible : le dossier doit être en statut SUBMITTED ou UNDER_REVIEW.");
        }

        // Vérifier que tous les champs nécessaires sont remplis
        if (!isCompleteForApproval(loan)) {
            throw new RuntimeException("Approbation impossible : tous les champs requis du dossier ne sont pas remplis.");
        }

        loan.setStatus(LoanApplicationStatus.APPROVED);
        loan.setDecidedAt(Instant.now());

        return mapToResponseDto(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponseDto rejectApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        // Autorisé uniquement aux conseillers
        if (!hasRole(currentUser, ROLE_CONSEILLER)) {
            throw new RuntimeException("Accès refusé à cette demande");
        }

        // Statut éligible: SUBMITTED ou UNDER_REVIEW
        if (!(loan.getStatus() == LoanApplicationStatus.SUBMITTED || loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW)) {
            throw new RuntimeException("Rejet impossible : le dossier doit être en statut SUBMITTED ou UNDER_REVIEW.");
        }

        loan.setStatus(LoanApplicationStatus.REJECTED);
        loan.setDecidedAt(Instant.now());

        return mapToResponseDto(loanRepository.save(loan));
    }

    // --- Helper Methods ---

    private String generateUniqueReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!loanRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Impossible de générer une référence unique de dossier.");
    }

    private User getRequiredUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private boolean isApplicant(LoanApplication loan, User user) {
        return loan.getApplicant() != null && loan.getApplicant().getId().equals(user.getId());
    }

    private boolean isAssignedAdvisor(LoanApplication loan, User user) {
        return loan.getAssignedAdvisor() != null && loan.getAssignedAdvisor().getId().equals(user.getId());
    }

    private void ensureCanAccessLoan(LoanApplication loan, User currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }
        if (hasRole(currentUser, ROLE_CONSEILLER) && isAssignedAdvisor(loan, currentUser)) {
            return;
        }
        if (isApplicant(loan, currentUser)) {
            return;
        }
        throw new RuntimeException("Accès refusé à cette demande");
    }

    private void ensureCanSubmit(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new RuntimeException(
                    "Soumission impossible : seul un dossier en brouillon (DRAFT) peut être soumis."
            );
        }
    }

    private void ensureCanUpdate(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new RuntimeException(
                    "Mise à jour impossible : seul un dossier en brouillon (DRAFT) peut être modifié."
            );
        }
    }

    private boolean isCompleteForApproval(LoanApplication loan) {
        return loan.getRequestedAmount() != null
                && loan.getRequestedDurationMonths() != null
                && loan.getPurpose() != null && !loan.getPurpose().isBlank()
                && loan.getMonthlyIncome() != null
                && loan.getEmploymentStatus() != null
                && loan.getAssignedAdvisor() != null
                && loan.getApprovedAmount() != null
                && loan.getApprovedDurationMonths() != null
                && loan.getInterestRate() != null;
    }

    private LoanResponseDto mapToResponseDto(LoanApplication loan) {
        return LoanResponseDto.builder()
                .id(loan.getId())
                .reference(loan.getReference())
                .status(loan.getStatus())
                .requestedAmount(loan.getRequestedAmount())
                .requestedDurationMonths(loan.getRequestedDurationMonths())
                .purpose(loan.getPurpose())
                .monthlyIncome(loan.getMonthlyIncome())
                .employmentStatus(loan.getEmploymentStatus())
                .submittedAt(loan.getSubmittedAt())
                .decidedAt(loan.getDecidedAt())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .decisionComment(loan.getDecisionComment())
                .approvedAmount(loan.getApprovedAmount())
                .approvedDurationMonths(loan.getApprovedDurationMonths())
                .interestRate(loan.getInterestRate())
                .applicantId(loan.getApplicant() != null ? loan.getApplicant().getId() : null)
                .applicantName(
                        loan.getApplicant() != null
                                ? (loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName()).trim()
                                : null
                )
                .advisorId(loan.getAssignedAdvisor() != null ? loan.getAssignedAdvisor().getId() : null)
                .advisorName(
                        loan.getAssignedAdvisor() != null
                                ? (loan.getAssignedAdvisor().getFirstName() + " " + loan.getAssignedAdvisor().getLastName()).trim()
                                : null
                )
                .build();
    }
}