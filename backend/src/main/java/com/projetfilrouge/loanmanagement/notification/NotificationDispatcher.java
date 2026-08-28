package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.service.NotificationService;
import com.projetfilrouge.loanmanagement.service.TransactionalEmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final LoanApplicationRepository loanApplicationRepository;
    private final NotificationService notificationService;
    private final TransactionalEmailNotificationService emailNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanApplicationEventRecorded(LoanApplicationEventRecorded payload) {
        dispatch(payload.event());
    }

    public void dispatch(LoanApplicationEvent event) {
        if (event == null || event.getLoanApplication() == null || event.getEventType() == null) {
            return;
        }

        LoanApplication loan = loanApplicationRepository.findByIdWithApplicantAndAdvisor(event.getLoanApplication().getId())
                .orElse(null);
        if (loan == null) {
            log.warn("Dossier {} introuvable pour notification {}", event.getLoanApplication().getId(), event.getEventType());
            return;
        }
        LoanApplicationEventType type = event.getEventType();
        NotificationContent clientContent = NotificationContentFactory.forEvent(type, loan, NotificationAudience.CLIENT);
        NotificationContent advisorContent = NotificationContentFactory.forEvent(type, loan, NotificationAudience.ADVISOR);

        try {
            if (NotificationPolicy.shouldSendEmail(type) && loan.getApplicant() != null) {
                emailNotificationService.sendToUser(loan.getApplicant(), clientContent);
            }
        } catch (Exception ex) {
            log.warn("Échec envoi e-mail notification {} pour dossier {}: {}",
                    type, loan.getId(), ex.getMessage());
        }

        try {
            if (NotificationPolicy.shouldCreateInAppForClient(type) && loan.getApplicant() != null) {
                notificationService.createInApp(loan.getApplicant(), type, clientContent, loan.getId());
                log.debug("Notification in-app client créée pour {} ({})", loan.getApplicant().getEmail(), type);
            }
        } catch (Exception ex) {
            log.error("Échec notification in-app client {} pour dossier {}: {}",
                    type, loan.getId(), ex.getMessage(), ex);
        }

        try {
            if (NotificationPolicy.shouldCreateInAppForAdvisor(type) && loan.getAssignedAdvisor() != null) {
                notificationService.createInApp(loan.getAssignedAdvisor(), type, advisorContent, loan.getId());
                log.debug("Notification in-app conseiller créée pour {} ({})",
                        loan.getAssignedAdvisor().getEmail(), type);
            }
        } catch (Exception ex) {
            log.error("Échec notification in-app conseiller {} pour dossier {}: {}",
                    type, loan.getId(), ex.getMessage(), ex);
        }
    }
}
