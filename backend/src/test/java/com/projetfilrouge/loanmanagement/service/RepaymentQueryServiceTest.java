package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.MandateStatus;
import com.projetfilrouge.loanmanagement.entity.PaymentMethod;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentTransactionRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentQueryServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private RepaymentPlanRepository repaymentPlanRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private DirectDebitMandateRepository mandateRepository;
    @Mock
    private LoanApplicationHistoryService historyService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RepaymentQueryService repaymentQueryService;

    private User borrower;
    private Loan loan;

    @BeforeEach
    void setUp() {
        borrower = User.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(Set.of(Role.builder().name("ROLE_CLIENT").build()))
                .build();
        LoanApplication application = LoanApplication.builder()
                .id(42L)
                .reference("LF-DEMO-0001")
                .applicant(borrower)
                .status(LoanApplicationStatus.APPROVED)
                .requestedAmount(new BigDecimal("15000"))
                .requestedDurationMonths(48)
                .title("Prêt démo")
                .loanPurpose(LoanPurpose.PERSONAL)
                .purpose("Projet")
                .monthlyIncome(new BigDecimal("3500"))
                .employmentStatus(EmploymentStatus.CDI)
                .build();
        loan = Loan.builder()
                .id(10L)
                .loanApplication(application)
                .borrower(borrower)
                .status(LoanStatus.ACTIVE)
                .principalAmount(new BigDecimal("15000"))
                .remainingBalance(new BigDecimal("14500"))
                .durationMonths(48)
                .annualRate(new BigDecimal("3.85"))
                .build();
    }

    @Test
    void getMyLoans_returnsSummariesForBorrower() {
        RepaymentPlan plan = RepaymentPlan.builder()
                .id(20L)
                .loan(loan)
                .monthlyPayment(new BigDecimal("337.68"))
                .installmentCount(48)
                .totalRepayable(new BigDecimal("16208.64"))
                .build();
        Installment next = Installment.builder()
                .repaymentPlan(plan)
                .sequenceNumber(3)
                .dueDate(LocalDate.now().plusDays(5))
                .amountDue(new BigDecimal("337.68"))
                .status(InstallmentStatus.UPCOMING)
                .build();

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(borrower));
        when(loanRepository.findByBorrowerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(loan));
        when(repaymentPlanRepository.findByLoanId(10L)).thenReturn(Optional.of(plan));
        when(installmentRepository.findFirstByRepaymentPlan_Loan_IdAndStatusInOrderByDueDateAsc(
                10L,
                List.of(InstallmentStatus.UPCOMING, InstallmentStatus.FAILED, InstallmentStatus.BLOCKED)
        )).thenReturn(Optional.of(next));
        when(installmentRepository.countByLoanIdAndStatus(10L, InstallmentStatus.OVERDUE)).thenReturn(0L);
        when(installmentRepository.countByLoanIdAndStatus(10L, InstallmentStatus.PAID)).thenReturn(2L);
        when(mandateRepository.findByLoanId(10L)).thenReturn(Optional.of(
                DirectDebitMandate.builder()
                        .status(MandateStatus.ACTIVE)
                        .loan(loan)
                        .paymentMethod(PaymentMethod.builder()
                                .ibanMasked("FR14 **** **** 2606")
                                .build())
                        .build()
        ));

        var summaries = repaymentQueryService.getMyLoans("client@test.com");

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getReference()).isEqualTo("LF-DEMO-0001");
        assertThat(summaries.get(0).getMandateStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getLoanDetailForBorrower_rejectsOtherBorrower() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> repaymentQueryService.getLoanDetailForBorrower(10L, "other@test.com"))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
