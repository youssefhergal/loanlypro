package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.service.AdvisorAssignmentService;
import com.projetfilrouge.loanmanagement.web.dto.response.AdvisorAssignmentResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.advisor-assignment.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AdvisorAssignmentScheduler {

    private final AdvisorAssignmentService advisorAssignmentService;

    @Value("${app.advisor-assignment.scheduler.cron:0 0 * * * *}")
    private String cronExpression;

    @PostConstruct
    void logSchedulerConfig() {
        log.info("Planificateur d'affectation conseillers actif — cron={}", cronExpression);
    }

    @Scheduled(cron = "${app.advisor-assignment.scheduler.cron:0 0 * * * *}")
    public void runScheduledAssignment() {
        log.info("Démarrage de l'affectation automatique des conseillers…");
        AdvisorAssignmentResultDto result = advisorAssignmentService.assignUnassignedApplications(
                null,
                AdvisorAssignmentService.Trigger.SCHEDULED
        );
        log.info(
                "Affectation automatique terminée : {} dossier(s) assigné(s), {} non affecté(s) restant(s).",
                result.getAssignedCount(),
                result.getUnassignedRemaining()
        );
    }
}
