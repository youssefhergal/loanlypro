package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto; // À créer
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto; // À créer
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

    private final LoanApplicationRepository loanRepository;
    private final UserRepository userRepository;

    @Transactional
    public LoanResponseDto createApplication(LoanRequestDto request, String currentUserEmail) {
        // Récupérer l'utilisateur (l'applicant) via son email (extrait du JWT)
        User applicant = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        LoanApplication loanApplication = LoanApplication.builder()
                .reference(generateUniqueReference())
                .applicant(applicant) // <--- Liaison cruciale
                .status(LoanApplicationStatus.DRAFT)
                .requestedAmount(request.getRequestedAmount())
                // ... reste du mapping
                .build();

        return mapToResponseDto(loanRepository.save(loanApplication));
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDto> getAllApplications() {
        return loanRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanResponseDto getApplicationByReference(String reference) {
        LoanApplication loan = loanRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Demande de prêt introuvable avec la référence : " + reference));
        return mapToResponseDto(loan);
    }

    @Transactional
    public LoanResponseDto submitApplication(Long id) {
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        loan.setStatus(LoanApplicationStatus.SUBMITTED);
        loan.setSubmittedAt(Instant.now());

        return mapToResponseDto(loanRepository.save(loan));
    }

    // --- Helper Methods ---

    private String generateUniqueReference() {
        return "LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private LoanResponseDto mapToResponseDto(LoanApplication loan) {
        return LoanResponseDto.builder()
                .id(loan.getId())
                .reference(loan.getReference())
                .status(loan.getStatus())
                .requestedAmount(loan.getRequestedAmount())
                .requestedDurationMonths(loan.getRequestedDurationMonths())
                .createdAt(loan.getCreatedAt())
                // Ajoute ici les autres champs nécessaires pour le client
                .build();
    }
}