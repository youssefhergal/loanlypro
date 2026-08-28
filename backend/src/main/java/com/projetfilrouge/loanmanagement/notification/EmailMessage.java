package com.projetfilrouge.loanmanagement.notification;

import java.util.List;

public record EmailMessage(
        List<String> to,
        String subject,
        String htmlBody,
        String textBody
) {
    public EmailMessage {
        if (to == null || to.isEmpty()) {
            throw new IllegalArgumentException("Destinataire requis.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Sujet requis.");
        }
    }

    public static EmailMessage of(String to, String subject, String htmlBody, String textBody) {
        return new EmailMessage(List.of(to), subject, htmlBody, textBody);
    }
}
