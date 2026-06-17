package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentSchedulerServiceTest {

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private DirectDebitExecutionService directDebitExecutionService;

    @InjectMocks
    private RepaymentSchedulerService repaymentSchedulerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repaymentSchedulerService, "maxAttempts", 3);
    }

    @Test
    void processDueInstallments_processesDueAndRetryCandidates() {
        LocalDate date = LocalDate.of(2026, 6, 1);
        Installment due = installment(1L);
        Installment retry = installment(2L);
        retry.setStatus(InstallmentStatus.FAILED);

        when(installmentRepository.findDueInstallments(date)).thenReturn(List.of(due));
        when(installmentRepository.findRetryInstallments(date, 3)).thenReturn(List.of(retry));
        when(directDebitExecutionService.executeInstallment(due))
                .thenReturn(DirectDebitExecutionService.ExecutionResult.of(
                        DirectDebitExecutionService.ExecutionOutcome.SUCCESS));
        when(directDebitExecutionService.executeInstallment(retry))
                .thenReturn(DirectDebitExecutionService.ExecutionResult.of(
                        DirectDebitExecutionService.ExecutionOutcome.FAILED));

        var result = repaymentSchedulerService.processDueInstallments(date);

        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        verify(directDebitExecutionService).executeInstallment(due);
        verify(directDebitExecutionService).executeInstallment(retry);
    }

    private static Installment installment(Long id) {
        Loan loan = Loan.builder().id(10L).status(LoanStatus.ACTIVE).build();
        RepaymentPlan plan = RepaymentPlan.builder().id(20L).loan(loan).build();
        Installment installment = Installment.builder()
                .id(id)
                .repaymentPlan(plan)
                .status(InstallmentStatus.UPCOMING)
                .build();
        return installment;
    }
}
