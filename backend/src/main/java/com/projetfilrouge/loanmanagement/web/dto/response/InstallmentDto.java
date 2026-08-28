package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class InstallmentDto {

    Long id;
    Integer sequenceNumber;
    LocalDate dueDate;
    BigDecimal amountDue;
    BigDecimal principalPart;
    BigDecimal interestPart;
    BigDecimal remainingBalance;
    String status;
    Integer attemptCount;
    LocalDate nextRetryDate;
}
