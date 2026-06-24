package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationContentFactoryTest {

    private final LoanApplication loan = LoanApplication.builder()
            .id(1L)
            .reference("LF-DEMO-0001")
            .status(LoanApplicationStatus.SUBMITTED)
            .build();

    @Test
    void advisorAssigned_usesClientMessageForClient() {
        NotificationContent content = NotificationContentFactory.forEvent(
                LoanApplicationEventType.ADVISOR_ASSIGNED,
                loan,
                NotificationAudience.CLIENT
        );

        assertThat(content.title()).isEqualTo("Conseiller assigné");
        assertThat(content.message()).contains("Un conseiller a été assigné");
    }

    @Test
    void advisorAssigned_usesAdvisorMessageForAdvisor() {
        NotificationContent content = NotificationContentFactory.forEvent(
                LoanApplicationEventType.ADVISOR_ASSIGNED,
                loan,
                NotificationAudience.ADVISOR
        );

        assertThat(content.title()).isEqualTo("Nouveau dossier affecté");
        assertThat(content.message()).isEqualTo("Le dossier LF-DEMO-0001 vous a été assigné.");
    }

    @Test
    void applicationSubmitted_usesAdvisorSpecificMessage() {
        NotificationContent content = NotificationContentFactory.forEvent(
                LoanApplicationEventType.APPLICATION_SUBMITTED,
                loan,
                NotificationAudience.ADVISOR
        );

        assertThat(content.title()).isEqualTo("Nouvelle demande soumise");
        assertThat(content.message()).contains("vient d'être soumis par le client");
    }
}
