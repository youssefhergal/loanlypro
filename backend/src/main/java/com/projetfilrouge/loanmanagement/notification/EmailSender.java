package com.projetfilrouge.loanmanagement.notification;

/**
 * Abstraction d'envoi d'e-mails transactionnels (Resend en prod, logging en dev/test).
 */
public interface EmailSender {

    void send(EmailMessage message);
}
