package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.service.RepaymentSchedulerService;
import com.projetfilrouge.loanmanagement.web.dto.response.RepaymentSchedulerRunResultDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.repayment.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InstallmentScheduler {

    private final RepaymentSchedulerService repaymentSchedulerService;

    @Value("${app.repayment.scheduler.cron:0 0 6 * * *}")
    private String cronExpression;

    @PostConstruct
    void logSchedulerConfig() {
        log.info("Planificateur de prélèvements actif — cron={}", cronExpression);
    }

    @Scheduled(cron = "${app.repayment.scheduler.cron:0 0 6 * * *}")
    public void runScheduledDebits() {
        LocalDate today = LocalDate.now();
        log.info("Démarrage du traitement automatique des échéances pour {}…", today);
        RepaymentSchedulerRunResultDto result = repaymentSchedulerService.processDueInstallments(today);
        log.info(
                "Prélèvements terminés : traités={}, succès={}, échecs={}, bloqués={}, ignorés={}, retards={}.",
                result.getProcessedCount(),
                result.getSuccessCount(),
                result.getFailedCount(),
                result.getBlockedCount(),
                result.getSkippedCount(),
                result.getOverdueCount()
        );
    }
}
