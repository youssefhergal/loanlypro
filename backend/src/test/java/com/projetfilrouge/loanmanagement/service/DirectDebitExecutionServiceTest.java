package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
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
import com.projetfilrouge.loanmanagement.entity.MandateStatus;
import com.projetfilrouge.loanmanagement.entity.PaymentMethod;
import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.payment.FakePaymentProvider;
import com.projetfilrouge.loanmanagement.payment.PaymentProvider;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentTransactionRepository;
import com.projetfilrouge.loanmanagement.security.IbanVaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class DirectDebitExecutionServiceTest {

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private DirectDebitMandateRepository mandateRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private MandateService mandateService;

    @Mock
    private LoanApplicationHistoryService historyService;

    @Mock
    private OverdueDetectionService overdueDetectionService;

    @Mock
    private IbanVaultService ibanVaultService;

    private PaymentProvider paymentProvider;

    @InjectMocks
    private DirectDebitExecutionService directDebitExecutionService;

    @BeforeEach
    void setUp() {
        paymentProvider = new FakePaymentProvider(true, 0.0, 0L);
        directDebitExecutionService = new DirectDebitExecutionService(
                installmentRepository,
                loanRepository,
                mandateRepository,
                transactionRepository,
                paymentProvider,
                mandateService,
                ibanVaultService,
                historyService,
                overdueDetectionService
        );
        ReflectionTestUtils.setField(directDebitExecutionService, "maxAttempts", 3);
        ReflectionTestUtils.setField(directDebitExecutionService, "retryDays", List.of(0, 3, 7));
    }

    @Test
    void executeInstallment_marksBlockedWhenMandateMissing() {
        Installment installment = sampleInstallment(InstallmentStatus.UPCOMING);
        when(mandateService.hasActiveMandate(10L)).thenReturn(false);

        DirectDebitExecutionService.ExecutionResult result =
                directDebitExecutionService.executeInstallment(installment);

        assertThat(result.outcome()).isEqualTo(DirectDebitExecutionService.ExecutionOutcome.BLOCKED);
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.BLOCKED);
        verify(installmentRepository).save(installment);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void executeInstallment_marksPaidOnSuccessfulDebit() {
        Installment installment = sampleInstallment(InstallmentStatus.UPCOMING);
        installment.setId(5L);
        Loan loan = installment.getRepaymentPlan().getLoan();
        loan.setStatus(LoanStatus.ACTIVE);

        when(mandateService.hasActiveMandate(10L)).thenReturn(true);
        when(mandateRepository.findByLoanIdAndStatus(10L, MandateStatus.ACTIVE))
                .thenReturn(Optional.of(activeMandate(loan)));
        when(transactionRepository.existsByInstallmentIdAndAttemptNumber(5L, 1)).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DirectDebitExecutionService.ExecutionResult result =
                directDebitExecutionService.executeInstallment(installment);

        assertThat(result.outcome()).isEqualTo(DirectDebitExecutionService.ExecutionOutcome.SUCCESS);
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(installment.getAttemptCount()).isEqualTo(1);
        assertThat(loan.getRemainingBalance()).isEqualByComparingTo("14500.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(overdueDetectionService).closeLoanIfFullyRepaid(loan, loan.getLoanApplication());
        verify(historyService).recordEvent(
                eq(loan.getLoanApplication()),
                eq(LoanApplicationEventType.PAYMENT_SUCCEEDED),
                eq(LoanEventActorType.SYSTEM),
                eq("system"),
                eq("Système"),
                any(Map.class)
        );
    }

    @Test
    void executeInstallment_schedulesRetryOnFailure() {
        paymentProvider = new FakePaymentProvider(false, 1.0, 0L);
        directDebitExecutionService = new DirectDebitExecutionService(
                installmentRepository,
                loanRepository,
                mandateRepository,
                transactionRepository,
                paymentProvider,
                mandateService,
                ibanVaultService,
                historyService,
                overdueDetectionService
        );
        ReflectionTestUtils.setField(directDebitExecutionService, "maxAttempts", 3);
        ReflectionTestUtils.setField(directDebitExecutionService, "retryDays", List.of(0, 3, 7));
        when(ibanVaultService.resolve(any())).thenAnswer(inv -> inv.getArgument(0));

        Installment installment = sampleInstallment(InstallmentStatus.UPCOMING);
        installment.setId(5L);
        installment.getRepaymentPlan().getLoan().setStatus(LoanStatus.ACTIVE);

        when(mandateService.hasActiveMandate(10L)).thenReturn(true);
        when(mandateRepository.findByLoanIdAndStatus(10L, MandateStatus.ACTIVE))
                .thenReturn(Optional.of(activeMandate(installment.getRepaymentPlan().getLoan())));
        when(transactionRepository.existsByInstallmentIdAndAttemptNumber(5L, 1)).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DirectDebitExecutionService.ExecutionResult result =
                directDebitExecutionService.executeInstallment(installment);

        assertThat(result.outcome()).isEqualTo(DirectDebitExecutionService.ExecutionOutcome.FAILED);
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.FAILED);
        assertThat(installment.getNextRetryDate()).isEqualTo(installment.getDueDate().plusDays(3));
        verify(historyService).recordEvent(
                any(),
                eq(LoanApplicationEventType.PAYMENT_FAILED),
                eq(LoanEventActorType.SYSTEM),
                eq("system"),
                eq("Système"),
                any(Map.class)
        );
    }

    @Test
    void executeInstallment_marksOverdueAfterMaxAttempts() {
        paymentProvider = new FakePaymentProvider(false, 1.0, 0L);
        directDebitExecutionService = new DirectDebitExecutionService(
                installmentRepository,
                loanRepository,
                mandateRepository,
                transactionRepository,
                paymentProvider,
                mandateService,
                ibanVaultService,
                historyService,
                overdueDetectionService
        );
        ReflectionTestUtils.setField(directDebitExecutionService, "maxAttempts", 3);
        ReflectionTestUtils.setField(directDebitExecutionService, "retryDays", List.of(0, 3, 7));
        when(ibanVaultService.resolve(any())).thenAnswer(inv -> inv.getArgument(0));

        Installment installment = sampleInstallment(InstallmentStatus.FAILED);
        installment.setId(5L);
        installment.setAttemptCount(2);
        Loan loan = installment.getRepaymentPlan().getLoan();
        loan.setStatus(LoanStatus.ACTIVE);

        when(mandateService.hasActiveMandate(10L)).thenReturn(true);
        when(mandateRepository.findByLoanIdAndStatus(10L, MandateStatus.ACTIVE))
                .thenReturn(Optional.of(activeMandate(loan)));
        when(transactionRepository.existsByInstallmentIdAndAttemptNumber(5L, 3)).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DirectDebitExecutionService.ExecutionResult result =
                directDebitExecutionService.executeInstallment(installment);

        assertThat(result.outcome()).isEqualTo(DirectDebitExecutionService.ExecutionOutcome.OVERDUE);
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
        verify(overdueDetectionService).onInstallmentOverdue(installment, loan, loan.getLoanApplication());
    }

    private static Installment sampleInstallment(InstallmentStatus status) {
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
                .status(LoanStatus.PENDING_MANDATE)
                .principalAmount(new BigDecimal("15000"))
                .durationMonths(48)
                .annualRate(new BigDecimal("3.85"))
                .remainingBalance(new BigDecimal("15000"))
                .build();
        RepaymentPlan plan = RepaymentPlan.builder()
                .id(20L)
                .loan(loan)
                .monthlyPayment(new BigDecimal("337.68"))
                .installmentCount(48)
                .installments(new java.util.ArrayList<>())
                .build();
        Installment installment = Installment.builder()
                .repaymentPlan(plan)
                .sequenceNumber(1)
                .dueDate(LocalDate.now())
                .amountDue(new BigDecimal("337.68"))
                .principalPart(new BigDecimal("289.68"))
                .interestPart(new BigDecimal("48.00"))
                .remainingBalance(new BigDecimal("14500.00"))
                .status(status)
                .attemptCount(0)
                .build();
        plan.getInstallments().add(installment);
        return installment;
    }

    private static DirectDebitMandate activeMandate(Loan loan) {
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .id(1L)
                .user(loan.getBorrower())
                .ibanMasked("FR76 **** **** 0185")
                .ibanToken("FR1420041010050500013M02606")
                .holderName("Jean Dupont")
                .build();
        return DirectDebitMandate.builder()
                .id(1L)
                .loan(loan)
                .paymentMethod(paymentMethod)
                .mandateReference("MND-TEST01")
                .status(MandateStatus.ACTIVE)
                .build();
    }
}
