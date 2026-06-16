package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueDetectionServiceTest {

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentPlanRepository repaymentPlanRepository;

    @Mock
    private LoanApplicationHistoryService historyService;

    @InjectMocks
    private OverdueDetectionService overdueDetectionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(overdueDetectionService, "overdueThreshold", 2);
    }

    @Test
    void onInstallmentOverdue_recordsEventAndChecksDefault() {
        LoanContext context = sampleContext(LoanStatus.ACTIVE);
        Installment installment = context.installments().get(0);
        installment.setStatus(InstallmentStatus.OVERDUE);

        overdueDetectionService.onInstallmentOverdue(installment, context.loan(), context.application());

        verify(historyService).recordEvent(
                eq(context.application()),
                eq(LoanApplicationEventType.INSTALLMENT_OVERDUE),
                eq(LoanEventActorType.SYSTEM),
                eq("system"),
                eq("Système"),
                any(Map.class)
        );
        verify(installmentRepository).countByLoanIdAndStatus(10L, InstallmentStatus.OVERDUE);
    }

    @Test
    void markLoanDefaultedIfNeeded_setsDefaultedWhenThresholdReached() {
        LoanContext context = sampleContext(LoanStatus.ACTIVE);
        when(installmentRepository.countByLoanIdAndStatus(10L, InstallmentStatus.OVERDUE)).thenReturn(2L);

        overdueDetectionService.markLoanDefaultedIfNeeded(context.loan(), context.application());

        assertThat(context.loan().getStatus()).isEqualTo(LoanStatus.DEFAULTED);
        verify(loanRepository).save(context.loan());
        verify(historyService).recordEvent(
                eq(context.application()),
                eq(LoanApplicationEventType.LOAN_DEFAULTED),
                eq(LoanEventActorType.SYSTEM),
                eq("system"),
                eq("Système"),
                any(Map.class)
        );
    }

    @Test
    void markLoanDefaultedIfNeeded_doesNothingBelowThreshold() {
        LoanContext context = sampleContext(LoanStatus.ACTIVE);
        when(installmentRepository.countByLoanIdAndStatus(10L, InstallmentStatus.OVERDUE)).thenReturn(1L);

        overdueDetectionService.markLoanDefaultedIfNeeded(context.loan(), context.application());

        assertThat(context.loan().getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(loanRepository, never()).save(any());
        verify(historyService, never()).recordEvent(
                any(), eq(LoanApplicationEventType.LOAN_DEFAULTED), any(), any(), any(), any()
        );
    }

    @Test
    void closeLoanIfFullyRepaid_closesLoanAndRecordsEvent() {
        LoanContext context = sampleContext(LoanStatus.ACTIVE);
        Installment first = context.installments().get(0);
        Installment second = context.installments().get(1);
        first.setStatus(InstallmentStatus.PAID);
        second.setStatus(InstallmentStatus.PAID);

        when(repaymentPlanRepository.findByLoanId(10L)).thenReturn(Optional.of(context.plan()));
        when(installmentRepository.findByRepaymentPlanIdOrderBySequenceNumberAsc(20L))
                .thenReturn(List.of(first, second));

        overdueDetectionService.closeLoanIfFullyRepaid(context.loan(), context.application());

        assertThat(context.loan().getStatus()).isEqualTo(LoanStatus.CLOSED);
        assertThat(context.loan().getRemainingBalance()).isEqualByComparingTo("0");
        assertThat(context.loan().getClosedAt()).isNotNull();
        verify(loanRepository).save(context.loan());
        verify(historyService).recordEvent(
                eq(context.application()),
                eq(LoanApplicationEventType.LOAN_CLOSED),
                eq(LoanEventActorType.SYSTEM),
                eq("system"),
                eq("Système"),
                any(Map.class)
        );
    }

    @Test
    void closeLoanIfFullyRepaid_keepsLoanOpenWhenInstallmentsRemain() {
        LoanContext context = sampleContext(LoanStatus.ACTIVE);
        Installment first = context.installments().get(0);
        Installment second = context.installments().get(1);
        first.setStatus(InstallmentStatus.PAID);
        second.setStatus(InstallmentStatus.UPCOMING);

        when(repaymentPlanRepository.findByLoanId(10L)).thenReturn(Optional.of(context.plan()));
        when(installmentRepository.findByRepaymentPlanIdOrderBySequenceNumberAsc(20L))
                .thenReturn(List.of(first, second));

        overdueDetectionService.closeLoanIfFullyRepaid(context.loan(), context.application());

        assertThat(context.loan().getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(loanRepository, never()).save(any());
        verify(historyService, never()).recordEvent(
                any(), eq(LoanApplicationEventType.LOAN_CLOSED), any(), any(), any(), any()
        );
    }

    private static LoanContext sampleContext(LoanStatus loanStatus) {
        User borrower = User.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(java.util.Set.of(Role.builder().name("ROLE_CLIENT").build()))
                .build();
        LoanApplication application = LoanApplication.builder()
                .id(42L)
                .reference("LF-2026-0042")
                .applicant(borrower)
                .status(LoanApplicationStatus.APPROVED)
                .requestedAmount(new BigDecimal("15000"))
                .requestedDurationMonths(48)
                .title("Prêt")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet")
                .monthlyIncome(new BigDecimal("3500"))
                .employmentStatus(EmploymentStatus.CDI)
                .build();
        Loan loan = Loan.builder()
                .id(10L)
                .loanApplication(application)
                .borrower(borrower)
                .status(loanStatus)
                .principalAmount(new BigDecimal("15000"))
                .durationMonths(48)
                .annualRate(new BigDecimal("3.85"))
                .remainingBalance(new BigDecimal("14500"))
                .build();
        RepaymentPlan plan = RepaymentPlan.builder()
                .id(20L)
                .loan(loan)
                .monthlyPayment(new BigDecimal("337.68"))
                .installmentCount(2)
                .totalRepayable(new BigDecimal("16208.32"))
                .installments(new java.util.ArrayList<>())
                .build();
        Installment first = Installment.builder()
                .id(1L)
                .repaymentPlan(plan)
                .sequenceNumber(1)
                .dueDate(LocalDate.now().minusMonths(1))
                .amountDue(new BigDecimal("337.68"))
                .principalPart(new BigDecimal("289.68"))
                .interestPart(new BigDecimal("48.00"))
                .remainingBalance(new BigDecimal("14710.32"))
                .status(InstallmentStatus.PAID)
                .build();
        Installment second = Installment.builder()
                .id(2L)
                .repaymentPlan(plan)
                .sequenceNumber(2)
                .dueDate(LocalDate.now())
                .amountDue(new BigDecimal("337.68"))
                .principalPart(new BigDecimal("290.61"))
                .interestPart(new BigDecimal("47.07"))
                .remainingBalance(new BigDecimal("14419.71"))
                .status(InstallmentStatus.UPCOMING)
                .build();
        plan.getInstallments().addAll(List.of(first, second));
        return new LoanContext(loan, application, plan, List.of(first, second));
    }

    private record LoanContext(
            Loan loan,
            LoanApplication application,
            RepaymentPlan plan,
            List<Installment> installments
    ) {
    }
}
