package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class LoanDetailDto {

    Long id;
    Long loanApplicationId;
    String reference;
    String status;
    BigDecimal principalAmount;
    BigDecimal remainingBalance;
    BigDecimal monthlyPayment;
    Integer durationMonths;
    BigDecimal annualRate;
    BigDecimal totalRepayable;
    Integer installmentCount;
    long paidInstallmentsCount;
    long overdueInstallmentsCount;
    LocalDate nextInstallmentDate;
    BigDecimal nextInstallmentAmount;
    String mandateStatus;
    String ibanMasked;
    String holderName;
    String borrowerName;
    String borrowerEmail;
    String advisorName;
    Instant activatedAt;
    Instant closedAt;
    List<InstallmentDto> installments;
    List<PaymentTransactionDto> transactions;
}
