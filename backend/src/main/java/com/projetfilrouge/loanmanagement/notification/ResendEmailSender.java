package com.projetfilrouge.loanmanagement.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ResendEmailSender implements EmailSender {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String fromAddress;

    public ResendEmailSender(String apiKey, String fromAddress) {
        this.fromAddress = fromAddress;
        this.restClient = RestClient.builder()
                .baseUrl(RESEND_API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void send(EmailMessage message) {
        Map<String, Object> body = new HashMap<>();
        body.put("from", fromAddress);
        body.put("to", message.to());
        body.put("subject", message.subject());
        if (message.htmlBody() != null) {
            body.put("html", message.htmlBody());
        }
        if (message.textBody() != null) {
            body.put("text", message.textBody());
        }

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Échec envoi email Resend to={} subject={}", message.to(), message.subject(), ex);
            throw new EmailDeliveryException("Impossible d'envoyer l'e-mail.", ex);
        }
    }
}
