package com.projetfilrouge.loanmanagement.notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.info(
                "[EMAIL-DEV] to={} subject={}\n---\n{}\n---",
                message.to(),
                message.subject(),
                message.textBody() != null ? message.textBody() : message.htmlBody()
        );
    }
}
