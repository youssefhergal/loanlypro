package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.Notification;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.notification.NotificationAudience;
import com.projetfilrouge.loanmanagement.notification.NotificationContent;
import com.projetfilrouge.loanmanagement.notification.NotificationContentFactory;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.NotificationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Notifications in-app de démo pour client@test.com et conseiller@test.com.
 * S'exécute après {@link DemoRepaymentSeeder} pour lier les alertes au dossier LF-DEMO-0001.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("demo")
@Order(40)
public class DemoNotificationSeeder implements CommandLineRunner {

    private static final String DEMO_REFERENCE = "LF-DEMO-0001";
    private static final String CLIENT_EMAIL = "client@test.com";
    private static final String ADVISOR_EMAIL = "conseiller@test.com";
    private static final String REFERENCE_LOAN_APPLICATION = "LOAN_APPLICATION";

    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        User client = userRepository.findByEmail(CLIENT_EMAIL).orElse(null);
        User advisor = userRepository.findByEmail(ADVISOR_EMAIL).orElse(null);
        if (client == null || advisor == null) {
            log.warn("Demo notification seed ignoré : comptes client/conseiller manquants.");
            return;
        }

        if (notificationRepository.countByRecipientId(client.getId()) > 0) {
            log.info("Demo notification seed : notifications client déjà présentes, skip.");
            return;
        }

        LoanApplication demoLoan = loanApplicationRepository.findByReference(DEMO_REFERENCE).orElse(null);
        if (demoLoan == null) {
            log.warn("Demo notification seed : dossier {} introuvable, notifications sans lien dossier.", DEMO_REFERENCE);
        }

        List<Notification> toSave = new ArrayList<>();
        Instant now = Instant.now();

        toSave.addAll(buildClientNotifications(client, demoLoan, now));
        toSave.addAll(buildAdvisorNotifications(advisor, demoLoan, now));

        notificationRepository.saveAll(toSave);

        long clientUnread = notificationRepository.countByRecipientIdAndReadAtIsNull(client.getId());
        long advisorUnread = notificationRepository.countByRecipientIdAndReadAtIsNull(advisor.getId());
        log.info(
                "Demo notification seed : {} notifications créées (client: {} dont {} non lues, conseiller: {} dont {} non lues).",
                toSave.size(),
                toSave.stream().filter(n -> n.getRecipient().getId().equals(client.getId())).count(),
                clientUnread,
                toSave.stream().filter(n -> n.getRecipient().getId().equals(advisor.getId())).count(),
                advisorUnread
        );
    }

    private List<Notification> buildClientNotifications(User client, LoanApplication loan, Instant now) {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(create(client, LoanApplicationEventType.APPLICATION_SUBMITTED, loan, now.minus(5, ChronoUnit.DAYS), null, NotificationAudience.CLIENT));
        notifications.add(create(client, LoanApplicationEventType.REVIEW_STARTED, loan, now.minus(4, ChronoUnit.DAYS), null, NotificationAudience.CLIENT));
        notifications.add(create(client, LoanApplicationEventType.APPLICATION_APPROVED, loan, now.minus(3, ChronoUnit.DAYS), null, NotificationAudience.CLIENT));
        notifications.add(create(client, LoanApplicationEventType.MANDATE_ACTIVATED, loan, now.minus(2, ChronoUnit.DAYS), null, NotificationAudience.CLIENT));
        notifications.add(create(client, LoanApplicationEventType.PAYMENT_SUCCEEDED, loan, now.minus(1, ChronoUnit.DAYS), now.minus(20, ChronoUnit.HOURS), NotificationAudience.CLIENT));
        notifications.add(create(client, LoanApplicationEventType.PAYMENT_FAILED, loan, now.minus(6, ChronoUnit.HOURS), null, NotificationAudience.CLIENT));
        return notifications;
    }

    private List<Notification> buildAdvisorNotifications(User advisor, LoanApplication loan, Instant now) {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(create(advisor, LoanApplicationEventType.APPLICATION_SUBMITTED, loan, now.minus(5, ChronoUnit.DAYS), now.minus(4, ChronoUnit.DAYS), NotificationAudience.ADVISOR));
        notifications.add(create(advisor, LoanApplicationEventType.DOCUMENT_UPLOADED, loan, now.minus(4, ChronoUnit.DAYS), null, NotificationAudience.ADVISOR));
        notifications.add(create(advisor, LoanApplicationEventType.ADVISOR_ASSIGNED, loan, now.minus(3, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), NotificationAudience.ADVISOR));
        notifications.add(create(advisor, LoanApplicationEventType.PAYMENT_FAILED, loan, now.minus(6, ChronoUnit.HOURS), null, NotificationAudience.ADVISOR));
        notifications.add(create(advisor, LoanApplicationEventType.INSTALLMENT_OVERDUE, loan, now.minus(2, ChronoUnit.HOURS), null, NotificationAudience.ADVISOR));
        return notifications;
    }

    private Notification create(
            User recipient,
            LoanApplicationEventType eventType,
            LoanApplication loan,
            Instant createdAt,
            Instant readAt,
            NotificationAudience audience
    ) {
        NotificationContent content = loan != null
                ? NotificationContentFactory.forEvent(eventType, loan, audience)
                : fallbackContent(eventType);

        return Notification.builder()
                .recipient(recipient)
                .eventType(eventType)
                .title(content.title())
                .message(content.message())
                .referenceType(loan != null ? REFERENCE_LOAN_APPLICATION : null)
                .referenceId(loan != null ? loan.getId() : null)
                .readAt(readAt)
                .createdAt(createdAt)
                .build();
    }

    private static NotificationContent fallbackContent(LoanApplicationEventType eventType) {
        return new NotificationContent(
                "Notification démo",
                "Alerte de démonstration — " + eventType.name()
        );
    }
}
