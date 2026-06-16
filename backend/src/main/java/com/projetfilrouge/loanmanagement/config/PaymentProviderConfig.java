package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.payment.FakePaymentProvider;
import com.projetfilrouge.loanmanagement.payment.PaymentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentProviderConfig {

    @Bean
    @ConditionalOnProperty(name = "app.payment.provider", havingValue = "fake", matchIfMissing = true)
    PaymentProvider fakePaymentProvider(
            @Value("${app.payment.fake.always-success:false}") boolean alwaysSuccess,
            @Value("${app.payment.fake.fail-rate:0.0}") double failRate,
            @Value("${app.payment.fake.delay-ms:0}") long delayMs
    ) {
        return new FakePaymentProvider(alwaysSuccess, failRate, delayMs);
    }
}
