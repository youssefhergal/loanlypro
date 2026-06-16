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
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentPlanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentPlanRepository repaymentPlanRepository;

    @Mock
    private LoanApplicationHistoryService historyService;

    @InjectMocks
    private RepaymentPlanService repaymentPlanService;

    @Test
    void createLoanFromApprovedApplication_createsLoanPlanAndInstallments() {
        LoanApplication application = approvedApplication();

        when(loanRepository.findByLoanApplicationId(42L)).thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(100L);
            return loan;
        });
        when(repaymentPlanRepository.save(any(RepaymentPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan loan = repaymentPlanService.createLoanFromApprovedApplication(application);

        assertThat(loan.getId()).isEqualTo(100L);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING_MANDATE);
        assertThat(loan.getPrincipalAmount()).isEqualByComparingTo("15000.00");
        assertThat(loan.getDurationMonths()).isEqualTo(48);
        assertThat(loan.getRemainingBalance()).isEqualByComparingTo("15000.00");

        ArgumentCaptor<RepaymentPlan> planCaptor = ArgumentCaptor.forClass(RepaymentPlan.class);
        verify(repaymentPlanRepository).save(planCaptor.capture());
        RepaymentPlan plan = planCaptor.getValue();

        assertThat(plan.getInstallmentCount()).isEqualTo(48);
        assertThat(plan.getMonthlyPayment()).isGreaterThan(BigDecimal.ZERO);
        assertThat(plan.getInstallments()).hasSize(48);
        assertThat(plan.getInstallments().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(plan.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.UPCOMING);
        assertThat(plan.getInstallments().get(47).getRemainingBalance()).isEqualByComparingTo("0.00");

        verify(historyService).recordEvent(
                eq(application),
                eq(LoanApplicationEventType.LOAN_CREATED),
                eq(LoanEventActorType.SYSTEM),
                eq("system"),
                eq("Système"),
                any(Map.class)
        );
    }

    @Test
    void createLoanFromApprovedApplication_isIdempotentWhenLoanAlreadyExists() {
        LoanApplication application = approvedApplication();
        Loan existing = Loan.builder().id(99L).status(LoanStatus.PENDING_MANDATE).build();

        when(loanRepository.findByLoanApplicationId(42L)).thenReturn(Optional.of(existing));

        Loan loan = repaymentPlanService.createLoanFromApprovedApplication(application);

        assertThat(loan.getId()).isEqualTo(99L);
        verify(loanRepository, never()).save(any());
        verify(repaymentPlanRepository, never()).save(any());
        verify(historyService, never()).recordEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createLoanFromApprovedApplication_throwsWhenNotApproved() {
        LoanApplication application = approvedApplication();
        application.setStatus(LoanApplicationStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> repaymentPlanService.createLoanFromApprovedApplication(application))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void calculateMonthlyPayment_matchesAmortizationFormula() {
        BigDecimal payment = repaymentPlanService.calculateMonthlyPayment(
                new BigDecimal("15000"),
                new BigDecimal("3.85"),
                48
        );

        assertThat(payment).isEqualByComparingTo("337.68");
    }

    @Test
    void buildInstallments_lastInstallmentClearsRemainingBalance() {
        BigDecimal principal = new BigDecimal("15000");
        BigDecimal annualRate = new BigDecimal("3.85");
        int months = 48;
        BigDecimal monthlyPayment = repaymentPlanService.calculateMonthlyPayment(principal, annualRate, months);

        List<Installment> installments = repaymentPlanService.buildInstallments(
                principal,
                annualRate,
                months,
                monthlyPayment
        );

        assertThat(installments).hasSize(48);
        BigDecimal totalPrincipal = installments.stream()
                .map(Installment::getPrincipalPart)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo("15000.00");
        assertThat(installments.get(installments.size() - 1).getRemainingBalance())
                .isEqualByComparingTo("0.00");
    }

    private static LoanApplication approvedApplication() {
        User client = User.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(java.util.Set.of(Role.builder().name("ROLE_CLIENT").build()))
                .build();
        User advisor = User.builder()
                .id(2L)
                .email("conseiller@test.com")
                .firstName("Marie")
                .lastName("Conseil")
                .build();

        return LoanApplication.builder()
                .id(42L)
                .reference("LF-2026-0042")
                .applicant(client)
                .assignedAdvisor(advisor)
                .status(LoanApplicationStatus.APPROVED)
                .requestedAmount(new BigDecimal("15000"))
                .requestedDurationMonths(48)
                .title("Prêt personnel")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet")
                .monthlyIncome(new BigDecimal("3500"))
                .employmentStatus(EmploymentStatus.CDI)
                .approvedAmount(new BigDecimal("15000"))
                .approvedDurationMonths(48)
                .interestRate(new BigDecimal("3.85"))
                .build();
    }
}
