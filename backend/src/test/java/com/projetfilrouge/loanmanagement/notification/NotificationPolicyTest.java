package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPolicyTest {

    @Test
    void shouldSendEmail_forSubmittedApplication() {
        assertThat(NotificationPolicy.shouldSendEmail(LoanApplicationEventType.APPLICATION_SUBMITTED)).isTrue();
    }

    @Test
    void shouldNotSendEmail_forDocumentUploaded() {
        assertThat(NotificationPolicy.shouldSendEmail(LoanApplicationEventType.DOCUMENT_UPLOADED)).isFalse();
    }

    @Test
    void shouldCreateInAppForAdvisor_forDocumentUploaded() {
        assertThat(NotificationPolicy.shouldCreateInAppForAdvisor(LoanApplicationEventType.DOCUMENT_UPLOADED)).isTrue();
    }

    @Test
    void shouldNotCreateInAppForClient_forDocumentUploaded() {
        assertThat(NotificationPolicy.shouldCreateInAppForClient(LoanApplicationEventType.DOCUMENT_UPLOADED)).isFalse();
    }

    @Test
    void shouldSendEmail_forPaymentFailed() {
        assertThat(NotificationPolicy.shouldSendEmail(LoanApplicationEventType.PAYMENT_FAILED)).isTrue();
    }
}
