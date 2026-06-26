package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProfilService profilService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanService loanService;
    private final RepaymentQueryService repaymentQueryService;

    @Transactional(readOnly = true)
    public DashboardResponse getMyDashboard() {
        // Récupérer l'email de l'utilisateur connecté
        String email = profilService.getCurrentUser().getEmail();

        // 1) Demandes du client
        List<LoanApplication> applications = loanApplicationRepository.findByApplicantEmail(email);
        // Trier par dates (création la plus récente d'abord si dispo sinon par id desc)
        applications.sort(Comparator
                .comparing(LoanApplication::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LoanApplication::getId)
                .reversed());

        List<LoanApplicationSummaryDto> demandes = applications.stream()
                .map(app -> LoanApplicationSummaryDto.builder()
                        .id(app.getId())
                        .reference(app.getReference())
                        .status(app.getStatus())
                        .title(app.getTitle())
                        .requestedAmount(app.getRequestedAmount())
                        .requestedDurationMonths(app.getRequestedDurationMonths())
                        .createdAt(app.getCreatedAt())
                        .submittedAt(app.getSubmittedAt())
                        .decidedAt(app.getDecidedAt())
                        .build())
                .toList();

        // 2) Documents du client (tous documents de toutes ses demandes)
        List<LoanDocumentResponseDto> documents = new ArrayList<>();
        for (LoanApplication app : applications) {
            documents.addAll(loanService.getDocuments(app.getId(), email));
        }

        // 3) Transactions du client (toutes transactions de tous ses prêts)
        List<PaymentTransactionDto> transactions = new ArrayList<>();
        List<LoanSummaryDto> myLoans = repaymentQueryService.getMyLoans(email);
        for (LoanSummaryDto loan : myLoans) {
            transactions.addAll(repaymentQueryService.getTransactionsForBorrower(loan.getId(), email));
        }

        return DashboardResponse.builder()
                .demandes(demandes)
                .documents(documents)
                .transactions(transactions)
                .build();
    }
}
