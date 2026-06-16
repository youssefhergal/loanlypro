package com.projetfilrouge.loanmanagement.payment;

import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FakePaymentProviderTest {

    @Test
    void debit_returnsSuccessWithExternalReference() {
        FakePaymentProvider provider = new FakePaymentProvider(true, 0.0, 0L);

        PaymentProvider.PaymentResult result = provider.debit(sampleRequest("FR1420041010050500013M02606"));

        assertThat(result.status()).isEqualTo(PaymentTransactionStatus.SUCCESS);
        assertThat(result.externalReference()).startsWith("FAKE-txn-");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void debit_returnsFailureForConfiguredFailIban() {
        FakePaymentProvider provider = new FakePaymentProvider(true, 0.0, 0L);

        PaymentProvider.PaymentResult result = provider.debit(
                sampleRequest(FakePaymentProvider.FAIL_IBAN_TOKEN)
        );

        assertThat(result.status()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("insufficient_funds");
    }

    @Test
    void debit_returnsFailureForInvalidAmount() {
        FakePaymentProvider provider = new FakePaymentProvider(true, 0.0, 0L);

        PaymentProvider.PaymentResult result = provider.debit(new PaymentProvider.DebitRequest(
                "mandate-1",
                "FR1420041010050500013M02606",
                BigDecimal.ZERO,
                "EUR",
                "installment-1-attempt-1"
        ));

        assertThat(result.status()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("invalid_amount");
    }

    @Test
    void debit_canSimulateRandomFailureWhenAlwaysSuccessDisabled() {
        FakePaymentProvider provider = new FakePaymentProvider(false, 1.0, 0L);

        PaymentProvider.PaymentResult result = provider.debit(sampleRequest("FR1420041010050500013M02606"));

        assertThat(result.status()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("insufficient_funds");
    }

    private static PaymentProvider.DebitRequest sampleRequest(String ibanToken) {
        return new PaymentProvider.DebitRequest(
                "mandate-1",
                ibanToken,
                new BigDecimal("312.50"),
                "EUR",
                "installment-1-attempt-1"
        );
    }
}
