package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";
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
    public Page<LoanResponseDto> getAllApplications(String currentUserEmail, LoanApplicationStatus status, int page, int size) {
        User currentUser = getRequiredUser(currentUserEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LoanApplication> loans;

        if (hasRole(currentUser, ROLE_ADMIN)) {
            loans = status == null
                    ? loanRepository.findAll(pageable)
                    : loanRepository.findByStatus(status, pageable);
        } else if (hasRole(currentUser, ROLE_CONSEILLER)) {
            loans = status == null
                    ? loanRepository.findByAssignedAdvisorId(currentUser.getId(), pageable)
                    : loanRepository.findByAssignedAdvisorIdAndStatus(currentUser.getId(), status, pageable);
        } else {
            loans = status == null
                    ? loanRepository.findByApplicantEmail(currentUserEmail, pageable)
                    : loanRepository.findByApplicantEmailAndStatus(currentUserEmail, status, pageable);
        }

        return loans.map(this::mapToResponseDto);
    }

    @Transactional(readOnly = true)
    public LoanResponseDto getApplicationByReference(String reference, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de prêt introuvable avec la référence : " + reference));
        ensureCanAccessLoan(loan, currentUser);
        return mapToResponseDto(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponseDto getApplicationById(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de prêt introuvable avec l'identifiant : " + id));
        ensureCanAccessLoan(loan, currentUser);
        return mapToResponseDto(loan);
    }

    @Transactional
    public LoanResponseDto updateDraftApplication(Long id, LoanRequestDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanEditDraft(loan);

        loan.setRequestedAmount(request.getRequestedAmount());
        loan.setRequestedDurationMonths(request.getRequestedDurationMonths());
        loan.setPurpose(request.getPurpose());
        loan.setMonthlyIncome(request.getMonthlyIncome());
        loan.setEmploymentStatus(request.getEmploymentStatus());

        return mapToResponseDto(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponseDto submitApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanSubmit(loan);

        loan.setStatus(LoanApplicationStatus.SUBMITTED);
        if (loan.getSubmittedAt() == null) {
            loan.setSubmittedAt(Instant.now());
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
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
        throw new ForbiddenOperationException("Accès refusé à cette demande");
    }

    private void ensureCanSubmit(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Soumission impossible : seul un dossier en brouillon (DRAFT) peut être soumis."
            );
        }
    }

    private void ensureCanEditDraft(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Modification impossible : seul un dossier en brouillon (DRAFT) peut être modifié."
            );
        }
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