package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.notification.EmailMessage;
import com.projetfilrouge.loanmanagement.notification.EmailSender;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailSender emailSender;

    @Value("${app.mail.verification.expiry-minutes:30}")
    private int expiryMinutes;

    @Value("${app.mail.verification.dev-fixed-code:}")
    private String devFixedCode;

    @Transactional
    public void issueVerificationCode(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        String code = generateCode();
        user.setEmailVerificationCode(code);
        user.setEmailVerificationExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        userRepository.save(user);
        sendVerificationEmail(user, code);
    }

    @Transactional
    public void verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (user.isEmailVerified()) {
            return;
        }

        String normalizedToken = token == null ? "" : token.trim();
        if (normalizedToken.isBlank()) {
            throw new BusinessRuleException("Code de vérification invalide.");
        }

        if (user.getEmailVerificationCode() == null
                || !user.getEmailVerificationCode().equals(normalizedToken)) {
            throw new BusinessRuleException("Code de vérification invalide.");
        }

        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Code expiré. Demandez un nouveau code.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (user.isEmailVerified()) {
            throw new BusinessRuleException("Cet e-mail est déjà vérifié.");
        }
        issueVerificationCode(user);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateCode() {
        if (devFixedCode != null && !devFixedCode.isBlank()) {
            return devFixedCode.trim();
        }
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private void sendVerificationEmail(User user, String code) {
        String subject = "Confirmez votre inscription LoanlyPro";
        String textBody = """
                Bonjour %s,

                Votre code de vérification LoanlyPro est : %s

                Ce code expire dans %d minutes.

                Si vous n'êtes pas à l'origine de cette inscription, ignorez ce message.
                """.formatted(user.getFirstName(), code, expiryMinutes);

        String htmlBody = """
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Votre code de vérification LoanlyPro est :</p>
                <p style="font-size:24px;font-weight:bold;letter-spacing:4px;">%s</p>
                <p>Ce code expire dans %d minutes.</p>
                """.formatted(user.getFirstName(), code, expiryMinutes);

        emailSender.send(EmailMessage.of(user.getEmail(), subject, htmlBody, textBody));
    }
}
