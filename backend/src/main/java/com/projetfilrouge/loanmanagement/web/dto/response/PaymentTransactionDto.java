package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentTransactionDto {

    Long id;
    Long installmentId;
    Integer sequenceNumber;
    Integer attemptNumber;
    BigDecimal amount;
    String status;
    String failureReason;
    String externalReference;
    Instant attemptedAt;
    Instant settledAt;
}
