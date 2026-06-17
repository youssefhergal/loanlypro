package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class LoanSummaryDto {

    Long id;
    Long loanApplicationId;
    String reference;
    String status;
    BigDecimal principalAmount;
    BigDecimal remainingBalance;
    BigDecimal monthlyPayment;
    LocalDate nextInstallmentDate;
    BigDecimal nextInstallmentAmount;
    String nextInstallmentStatus;
    Integer installmentCount;
    Integer remainingInstallmentsCount;
    String mandateStatus;
    String borrowerName;
    String borrowerEmail;
    long overdueInstallmentsCount;
}
