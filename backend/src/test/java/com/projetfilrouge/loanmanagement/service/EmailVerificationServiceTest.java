package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.notification.EmailMessage;
import com.projetfilrouge.loanmanagement.notification.EmailSender;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSender emailSender;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(userRepository, emailSender);
        ReflectionTestUtils.setField(emailVerificationService, "expiryMinutes", 30);
        ReflectionTestUtils.setField(emailVerificationService, "devFixedCode", "000000");
    }

    @Test
    void issueVerificationCode_persistsCodeAndSendsEmail() {
        User user = unverifiedUser();

        emailVerificationService.issueVerificationCode(user);

        assertThat(user.getEmailVerificationCode()).isEqualTo("000000");
        assertThat(user.getEmailVerificationExpiresAt()).isAfter(Instant.now());
        verify(userRepository).save(user);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().to()).containsExactly("client@test.com");
        assertThat(messageCaptor.getValue().subject()).contains("LoanlyPro");
    }

    @Test
    void verifyEmail_marksUserVerifiedWhenCodeMatches() {
        User user = unverifiedUser();
        user.setEmailVerificationCode("000000");
        user.setEmailVerificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));

        emailVerificationService.verifyEmail("client@test.com", "000000");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationCode()).isNull();
        assertThat(user.getEmailVerificationExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_throwsWhenCodeIsWrong() {
        User user = unverifiedUser();
        user.setEmailVerificationCode("111111");
        user.setEmailVerificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> emailVerificationService.verifyEmail("client@test.com", "000000"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Code de vérification invalide.");

        verify(userRepository, never()).save(any());
    }

    private static User unverifiedUser() {
        return User.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .emailVerified(false)
                .build();
    }
}
