package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.RepaymentSchedulerRunResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentSchedulerService {

    private final InstallmentRepository installmentRepository;
    private final DirectDebitExecutionService directDebitExecutionService;

    @Value("${app.repayment.scheduler.max-attempts:3}")
    private int maxAttempts;

    @Transactional
    public RepaymentSchedulerRunResultDto processDueInstallments(LocalDate processingDate) {
        List<Installment> dueInstallments = installmentRepository.findDueInstallments(processingDate);
        List<Installment> retryInstallments = installmentRepository.findRetryInstallments(processingDate, maxAttempts);

        Set<Long> seenIds = new LinkedHashSet<>();
        List<Installment> toProcess = new ArrayList<>();
        for (Installment installment : dueInstallments) {
            if (seenIds.add(installment.getId())) {
                toProcess.add(installment);
            }
        }
        for (Installment installment : retryInstallments) {
            if (seenIds.add(installment.getId())) {
                toProcess.add(installment);
            }
        }

        int success = 0;
        int failed = 0;
        int blocked = 0;
        int skipped = 0;
        int overdue = 0;

        log.info("Traitement des échéances pour {} — {} échéance(s) candidate(s).", processingDate, toProcess.size());

        for (Installment installment : toProcess) {
            DirectDebitExecutionService.ExecutionResult result =
                    directDebitExecutionService.executeInstallment(installment);
            switch (result.outcome()) {
                case SUCCESS -> success++;
                case FAILED -> failed++;
                case OVERDUE -> {
                    failed++;
                    overdue++;
                }
                case BLOCKED -> blocked++;
                case SKIPPED -> skipped++;
            }
        }

        return RepaymentSchedulerRunResultDto.builder()
                .processedCount(toProcess.size())
                .successCount(success)
                .failedCount(failed)
                .blockedCount(blocked)
                .skippedCount(skipped)
                .overdueCount(overdue)
                .build();
    }
}
