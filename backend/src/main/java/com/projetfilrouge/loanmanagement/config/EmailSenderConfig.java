package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.notification.EmailSender;
import com.projetfilrouge.loanmanagement.notification.LoggingEmailSender;
import com.projetfilrouge.loanmanagement.notification.ResendEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailSenderConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "logging", matchIfMissing = true)
    EmailSender loggingEmailSender() {
        return new LoggingEmailSender();
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
    EmailSender resendEmailSender(
            @Value("${app.mail.resend.api-key:}") String apiKey,
            @Value("${app.mail.from:LoanlyFans <noreply@loanlyfans.fr>}") String fromAddress
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.mail.resend.api-key requis lorsque app.mail.provider=resend");
        }
        return new ResendEmailSender(apiKey, fromAddress);
    }
}
