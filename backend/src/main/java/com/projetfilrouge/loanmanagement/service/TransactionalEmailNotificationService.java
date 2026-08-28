package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.notification.EmailMessage;
import com.projetfilrouge.loanmanagement.notification.EmailSender;
import com.projetfilrouge.loanmanagement.notification.NotificationContent;
import com.projetfilrouge.loanmanagement.notification.NotificationContentFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionalEmailNotificationService {

    private final EmailSender emailSender;

    public void sendToUser(User recipient, NotificationContent content) {
        if (recipient == null || recipient.getEmail() == null) {
            return;
        }
        String textBody = content.title() + "\n\n" + content.message();
        String htmlBody = "<p><strong>" + content.title() + "</strong></p><p>" + content.message() + "</p>";
        emailSender.send(EmailMessage.of(recipient.getEmail(), content.title(), htmlBody, textBody));
    }

    public void sendForLoanEvent(LoanApplicationEvent event, LoanApplication loan) {
        NotificationContent content = NotificationContentFactory.forEvent(event.getEventType(), loan);
        sendToUser(loan.getApplicant(), content);
    }
}
