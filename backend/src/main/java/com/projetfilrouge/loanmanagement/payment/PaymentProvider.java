package com.projetfilrouge.loanmanagement.payment;

import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;

import java.math.BigDecimal;

public interface PaymentProvider {

    PaymentResult debit(DebitRequest request);

    record DebitRequest(
            String mandateReference,
            String ibanToken,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {}

    record PaymentResult(
            PaymentTransactionStatus status,
            String externalReference,
            String failureReason
    ) {
        public static PaymentResult success(String externalReference) {
            return new PaymentResult(PaymentTransactionStatus.SUCCESS, externalReference, null);
        }

        public static PaymentResult failure(String failureReason) {
            return new PaymentResult(PaymentTransactionStatus.FAILED, null, failureReason);
        }
    }
}
