package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private List<LoanApplicationSummaryDto> demandes;
    private List<LoanSummaryDto> loans;
    private List<JustificatifGroupResponseDto> justificatifs;
    private List<NotificationResponseDto> notifications;
    private long unreadNotificationsCount;
    private List<LoanDocumentResponseDto> documents;
    private List<PaymentTransactionDto> transactions;
}
