package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.*;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.NotificationRepository;
import com.projetfilrouge.loanmanagement.repository.RoleRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.service.LoanApplicationHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationDispatchIntegrationTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LoanApplicationHistoryService historyService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    private User client;
    private LoanApplication loan;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        loanApplicationRepository.deleteAll();
        userRepository.deleteAll();

        transactionTemplate.executeWithoutResult(status -> {
            Role clientRole = roleRepository.findByName("ROLE_CLIENT")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CLIENT").build()));

            client = userRepository.save(User.builder()
                    .email("notif-client@test.com")
                    .passwordHash("hash")
                    .firstName("Notif")
                    .lastName("Client")
                    .roles(new HashSet<>(Set.of(clientRole)))
                    .build());

            loan = loanApplicationRepository.save(LoanApplication.builder()
                    .reference("LF-NOTIF-001")
                    .status(LoanApplicationStatus.SUBMITTED)
                    .applicant(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .requestedDurationMonths(48)
                    .title("Prêt test notifications")
                    .loanPurpose(LoanPurpose.PERSONAL)
                    .purpose("Projet personnel")
                    .monthlyIncome(BigDecimal.valueOf(3_000))
                    .employmentStatus(EmploymentStatus.CDI)
                    .build());
        });
    }

    @Test
    void recordEvent_createsInAppNotificationAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> historyService.recordEvent(
                loan,
                LoanApplicationEventType.REVIEW_STARTED,
                LoanEventActorType.ADVISOR,
                "advisor@test.com",
                "Conseiller Test",
                Map.of()
        ));

        assertThat(notificationRepository.countByRecipientId(client.getId())).isEqualTo(1);
        assertThat(notificationRepository.countByRecipientIdAndReadAtIsNull(client.getId())).isEqualTo(1);
    }
}
