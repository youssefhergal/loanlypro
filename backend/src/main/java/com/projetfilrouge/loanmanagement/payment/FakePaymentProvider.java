package com.projetfilrouge.loanmanagement.payment;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class FakePaymentProvider implements PaymentProvider {

    public static final String FAIL_IBAN_TOKEN = "FR7630001007941234567890185";

    private final boolean alwaysSuccess;
    private final double failRate;
    private final long delayMs;

    public FakePaymentProvider(boolean alwaysSuccess, double failRate, long delayMs) {
        this.alwaysSuccess = alwaysSuccess;
        this.failRate = Math.max(0.0, Math.min(1.0, failRate));
        this.delayMs = Math.max(0L, delayMs);
    }

    @Override
    public PaymentResult debit(DebitRequest request) {
        simulateDelay();
        log.info(
                "FakePaymentProvider debit idempotencyKey={} amount={} {}",
                request.idempotencyKey(),
                request.amount(),
                request.currency()
        );

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentResult.failure("invalid_amount");
        }

        if (FAIL_IBAN_TOKEN.equalsIgnoreCase(normalizeIban(request.ibanToken()))) {
            return PaymentResult.failure("insufficient_funds");
        }

        if (alwaysSuccess || !shouldFail()) {
            return PaymentResult.success("FAKE-txn-" + UUID.randomUUID().toString().substring(0, 8));
        }

        return PaymentResult.failure("insufficient_funds");
    }

    private boolean shouldFail() {
        if (failRate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < failRate;
    }

    private void simulateDelay() {
        if (delayMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String normalizeIban(String ibanToken) {
        if (ibanToken == null) {
            return "";
        }
        return ibanToken.replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
