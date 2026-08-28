package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.MandateStatus;
import com.projetfilrouge.loanmanagement.entity.PaymentMethod;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentMethodRepository;
import com.projetfilrouge.loanmanagement.security.IbanVaultService;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MandateServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private DirectDebitMandateRepository mandateRepository;
    @Mock
    private IbanVaultService ibanVaultService;
    @Mock
    private LoanApplicationHistoryService historyService;

    @InjectMocks
    private MandateService mandateService;

    private Loan loan;
    private User borrower;

    @BeforeEach
    void setUp() {
        borrower = User.builder().id(1L).email("client@test.com").firstName("Jean").lastName("Dupont").build();
        LoanApplication application = LoanApplication.builder().id(5L).reference("LF-DEMO-0001").applicant(borrower).build();
        loan = Loan.builder()
                .id(10L)
                .status(LoanStatus.PENDING_MANDATE)
                .borrower(borrower)
                .loanApplication(application)
                .build();
    }

    @Test
    void activateMandate_storesVaultTokenAndActivatesLoan() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(mandateRepository.findByLoanId(10L)).thenReturn(Optional.empty());
        when(ibanVaultService.tokenize("FR1420041010050500013M02606")).thenReturn("v1:token");
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(inv -> {
            PaymentMethod method = inv.getArgument(0);
            method.setId(99L);
            return method;
        });
        when(mandateRepository.save(any(DirectDebitMandate.class))).thenAnswer(inv -> inv.getArgument(0));

        DirectDebitMandate mandate = mandateService.registerPaymentMethodAndActivateMandate(
                10L,
                "client@test.com",
                "FR14 2004 1010 0505 0001 3M02 606",
                "Jean Dupont"
        );

        assertThat(mandate.getStatus()).isEqualTo(MandateStatus.ACTIVE);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(historyService).recordEvent(
                eq(loan.getLoanApplication()),
                eq(LoanApplicationEventType.MANDATE_ACTIVATED),
                eq(LoanEventActorType.CLIENT),
                eq("client@test.com"),
                eq("Jean Dupont"),
                any(Map.class)
        );
    }

    @Test
    void activateMandate_rejectsForeignBorrower() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> mandateService.registerPaymentMethodAndActivateMandate(
                10L,
                "other@test.com",
                "FR1420041010050500013M02606",
                "Jean Dupont"
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void revokeMandate_marksMandateRevoked() {
        PaymentMethod method = PaymentMethod.builder().id(99L).ibanToken("v1:token").ibanMasked("FR14 ****").holderName("Jean").user(borrower).build();
        DirectDebitMandate mandate = DirectDebitMandate.builder()
                .loan(loan)
                .paymentMethod(method)
                .status(MandateStatus.ACTIVE)
                .mandateReference("MND-TEST01")
                .build();

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(mandateRepository.findByLoanIdAndStatus(10L, MandateStatus.ACTIVE)).thenReturn(Optional.of(mandate));
        when(mandateRepository.save(mandate)).thenReturn(mandate);

        DirectDebitMandate revoked = mandateService.revokeMandate(10L, "client@test.com");

        assertThat(revoked.getStatus()).isEqualTo(MandateStatus.REVOKED);
        assertThat(revoked.getRevokedAt()).isNotNull();
        verify(historyService).recordEvent(
                eq(loan.getLoanApplication()),
                eq(LoanApplicationEventType.MANDATE_REVOKED),
                eq(LoanEventActorType.CLIENT),
                eq("client@test.com"),
                eq("Jean Dupont"),
                any(Map.class)
        );
    }

    @Test
    void revokeMandate_withoutActiveMandate_throws() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(mandateRepository.findByLoanIdAndStatus(10L, MandateStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mandateService.revokeMandate(10L, "client@test.com"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
