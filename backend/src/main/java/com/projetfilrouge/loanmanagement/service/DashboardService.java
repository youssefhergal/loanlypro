package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
    private final ClientDocumentsService clientDocumentsService;
    private final NotificationService notificationService;

    private static final int DASHBOARD_NOTIFICATIONS_LIMIT = 10;

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

        // 3) Prêts, transactions, justificatifs et notifications
        List<LoanSummaryDto> myLoans = repaymentQueryService.getMyLoans(email);
        List<PaymentTransactionDto> transactions = new ArrayList<>();
        for (LoanSummaryDto loan : myLoans) {
            transactions.addAll(repaymentQueryService.getTransactionsForBorrower(loan.getId(), email));
        }

        List<JustificatifGroupResponseDto> justificatifs =
                clientDocumentsService.getJustificatifsGroupedByApplication(email);

        Page<NotificationResponseDto> notificationPage = notificationService.getMyNotifications(
                email,
                PageRequest.of(0, DASHBOARD_NOTIFICATIONS_LIMIT)
        );
        long unreadNotificationsCount = notificationService.countUnread(email);

        return DashboardResponse.builder()
                .demandes(demandes)
                .loans(myLoans)
                .justificatifs(justificatifs)
                .notifications(notificationPage.getContent())
                .unreadNotificationsCount(unreadNotificationsCount)
                .documents(documents)
                .transactions(transactions)
                .build();
    }

    @Transactional(readOnly = true)
    public AdvisorDashboardResponse getAdvisorDashboard() {
        var currentUser = profilService.getCurrentUser();
        String email = currentUser.getEmail();
        Page<?> page = repaymentQueryService.getAdvisorLoans(
                email,
                null,
                false,
                PageRequest.of(0, 5)
        );

        // Page is of LoanSummaryDto, but keep generic local type to avoid extra imports confusion
        @SuppressWarnings("unchecked")
        Page<LoanSummaryDto> loanPage = (Page<LoanSummaryDto>) page;

        // Récupérer également les LoanApplication assignées au conseiller (vérification demandée)
        List<LoanApplication> assignedApps = new ArrayList<>();
        if (currentUser.getId() != null) {
            assignedApps = loanApplicationRepository.findByAssignedAdvisorId(currentUser.getId());
            // Trier par soumission/creation récentes d'abord
            assignedApps.sort(Comparator
                    .comparing(LoanApplication::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LoanApplication::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LoanApplication::getId)
                    .reversed());
        }

        List<LoanApplicationSummaryDto> applications = assignedApps.stream()
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

        return AdvisorDashboardResponse.builder()
                .totalCount(loanPage.getTotalElements())
                .loans(loanPage.getContent())
                .applicationsCount(applications.size())
                .applications(applications)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminLoanListSummaryDto getAdminDashboard(String search) {
        String email = profilService.getCurrentUser().getEmail();
        return loanService.getAdminListSummary(email, search);
    }
}
