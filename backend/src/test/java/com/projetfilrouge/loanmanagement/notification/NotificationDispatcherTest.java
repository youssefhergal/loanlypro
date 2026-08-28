package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.service.NotificationService;
import com.projetfilrouge.loanmanagement.service.TransactionalEmailNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionalEmailNotificationService emailNotificationService;

    @InjectMocks
    private NotificationDispatcher notificationDispatcher;

    @Test
    void dispatch_sendsEmailAndCreatesInAppForClientOnSubmitted() {
        User applicant = User.builder().id(1L).email("client@test.com").build();
        LoanApplication loan = LoanApplication.builder()
                .id(10L)
                .reference("LF-001")
                .status(LoanApplicationStatus.SUBMITTED)
                .applicant(applicant)
                .build();
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .loanApplication(loan)
                .eventType(LoanApplicationEventType.APPLICATION_SUBMITTED)
                .build();

        when(loanApplicationRepository.findByIdWithApplicantAndAdvisor(10L)).thenReturn(Optional.of(loan));

        notificationDispatcher.dispatch(event);

        verify(emailNotificationService).sendToUser(eq(applicant), any(NotificationContent.class));
        verify(notificationService).createInApp(eq(applicant), eq(LoanApplicationEventType.APPLICATION_SUBMITTED),
                any(NotificationContent.class), eq(10L));
    }

    @Test
    void dispatch_skipsEmailForDocumentUploadedButNotifiesAdvisorInApp() {
        User applicant = User.builder().id(1L).email("client@test.com").build();
        User advisor = User.builder().id(2L).email("advisor@test.com").build();
        LoanApplication loan = LoanApplication.builder()
                .id(10L)
                .reference("LF-001")
                .status(LoanApplicationStatus.UNDER_REVIEW)
                .applicant(applicant)
                .assignedAdvisor(advisor)
                .build();
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .loanApplication(loan)
                .eventType(LoanApplicationEventType.DOCUMENT_UPLOADED)
                .build();

        when(loanApplicationRepository.findByIdWithApplicantAndAdvisor(10L)).thenReturn(Optional.of(loan));

        notificationDispatcher.dispatch(event);

        verify(emailNotificationService, never()).sendToUser(any(), any());
        verify(notificationService, never()).createInApp(eq(applicant), any(), any(), any());
        verify(notificationService).createInApp(eq(advisor), eq(LoanApplicationEventType.DOCUMENT_UPLOADED),
                any(NotificationContent.class), eq(10L));
    }

    @Test
    void dispatch_usesAdvisorSpecificContentOnAssignment() {
        User applicant = User.builder().id(1L).email("client@test.com").build();
        User advisor = User.builder().id(2L).email("advisor@test.com").build();
        LoanApplication loan = LoanApplication.builder()
                .id(10L)
                .reference("LF-001")
                .status(LoanApplicationStatus.SUBMITTED)
                .applicant(applicant)
                .assignedAdvisor(advisor)
                .build();
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .loanApplication(loan)
                .eventType(LoanApplicationEventType.ADVISOR_ASSIGNED)
                .build();

        when(loanApplicationRepository.findByIdWithApplicantAndAdvisor(10L)).thenReturn(Optional.of(loan));

        notificationDispatcher.dispatch(event);

        verify(notificationService).createInApp(eq(applicant), eq(LoanApplicationEventType.ADVISOR_ASSIGNED),
                eq(new NotificationContent(
                        "Conseiller assigné",
                        "Un conseiller a été assigné au dossier LF-001."
                )), eq(10L));
        verify(notificationService).createInApp(eq(advisor), eq(LoanApplicationEventType.ADVISOR_ASSIGNED),
                eq(new NotificationContent(
                        "Nouveau dossier affecté",
                        "Le dossier LF-001 vous a été assigné."
                )), eq(10L));
    }

    @Test
    void dispatch_notifiesAdvisorOnOfferAccepted() {
        User applicant = User.builder().id(1L).email("client@test.com").build();
        User advisor = User.builder().id(2L).email("advisor@test.com").build();
        LoanApplication loan = LoanApplication.builder()
                .id(10L)
                .reference("LF-001")
                .status(LoanApplicationStatus.UNDER_REVIEW)
                .applicant(applicant)
                .assignedAdvisor(advisor)
                .build();
        LoanApplicationEvent event = LoanApplicationEvent.builder()
                .loanApplication(loan)
                .eventType(LoanApplicationEventType.OFFER_ACCEPTED)
                .build();

        when(loanApplicationRepository.findByIdWithApplicantAndAdvisor(10L)).thenReturn(Optional.of(loan));

        notificationDispatcher.dispatch(event);

        verify(notificationService, never()).createInApp(eq(applicant), any(), any(), any());
        verify(notificationService).createInApp(eq(advisor), eq(LoanApplicationEventType.OFFER_ACCEPTED),
                eq(new NotificationContent(
                        "Contre-offre acceptée",
                        "Le client a accepté votre contre-offre pour le dossier LF-001."
                )), eq(10L));
    }
}
