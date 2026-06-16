package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import com.projetfilrouge.loanmanagement.entity.PaymentTransaction;
import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.entity.Role;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.InstallmentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.PaymentTransactionRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.InstallmentDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDetailDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.PaymentTransactionDto;
import com.projetfilrouge.loanmanagement.web.dto.response.RepaymentKpiDto;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepaymentQueryService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";

    private static final List<LoanStatus> OUTSTANDING_STATUSES = List.of(
            LoanStatus.PENDING_MANDATE,
            LoanStatus.ACTIVE,
            LoanStatus.DEFAULTED
    );

    private static final List<InstallmentStatus> NEXT_INSTALLMENT_STATUSES = List.of(
            InstallmentStatus.UPCOMING,
            InstallmentStatus.FAILED,
            InstallmentStatus.BLOCKED
    );

    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final DirectDebitMandateRepository mandateRepository;
    private final LoanApplicationHistoryService historyService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LoanSummaryDto> getMyLoans(String borrowerEmail) {
        User borrower = requireUser(borrowerEmail);
        return loanRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId()).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanDetailDto getLoanDetailForBorrower(Long loanId, String borrowerEmail) {
        Loan loan = requireLoan(loanId);
        assertBorrowerAccess(loan, borrowerEmail);
        return toDetailDto(loan, false, false);
    }

    @Transactional(readOnly = true)
    public List<InstallmentDto> getInstallmentsForBorrower(Long loanId, String borrowerEmail) {
        Loan loan = requireLoan(loanId);
        assertBorrowerAccess(loan, borrowerEmail);
        return loadInstallments(loanId);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getTransactionsForBorrower(Long loanId, String borrowerEmail) {
        Loan loan = requireLoan(loanId);
        assertBorrowerAccess(loan, borrowerEmail);
        return loadTransactions(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getRepaymentHistoryForBorrower(Long loanId, String borrowerEmail) {
        Loan loan = requireLoan(loanId);
        assertBorrowerAccess(loan, borrowerEmail);
        return historyService.getRepaymentHistory(loan.getLoanApplication().getId());
    }

    @Transactional(readOnly = true)
    public Page<LoanSummaryDto> getAdvisorLoans(
            String advisorEmail,
            LoanStatus status,
            boolean overdueOnly,
            Pageable pageable
    ) {
        User advisor = requireAdvisor(advisorEmail);
        Page<Loan> loans = findAdvisorLoans(advisor.getId(), status, overdueOnly, pageable);
        return loans.map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public LoanDetailDto getLoanDetailForAdvisor(Long loanId, String advisorEmail) {
        User advisor = requireAdvisor(advisorEmail);
        Loan loan = requireLoan(loanId);
        assertAdvisorAccess(loan, advisor);
        return toDetailDto(loan, true, true);
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getRepaymentHistoryForAdvisor(Long loanId, String advisorEmail) {
        User advisor = requireAdvisor(advisorEmail);
        Loan loan = requireLoan(loanId);
        assertAdvisorAccess(loan, advisor);
        return historyService.getRepaymentHistory(loan.getLoanApplication().getId());
    }

    @Transactional(readOnly = true)
    public RepaymentKpiDto getAdminKpi(String adminEmail) {
        requireAdmin(adminEmail);
        long activeCount = loanRepository.countByStatus(LoanStatus.ACTIVE)
                + loanRepository.countByStatus(LoanStatus.PENDING_MANDATE)
                + loanRepository.countByStatus(LoanStatus.DEFAULTED);
        long closedCount = loanRepository.countByStatus(LoanStatus.CLOSED);
        BigDecimal outstanding = loanRepository.sumRemainingBalanceByStatusIn(OUTSTANDING_STATUSES);

        YearMonth currentMonth = YearMonth.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        BigDecimal collected = paymentTransactionRepository.sumSuccessfulAmountBetween(monthStart, monthEnd);

        long successCount = paymentTransactionRepository.countByStatus(PaymentTransactionStatus.SUCCESS);
        long failedCount = paymentTransactionRepository.countByStatus(PaymentTransactionStatus.FAILED);
        long totalAttempts = successCount + failedCount;
        BigDecimal failureRate = totalAttempts == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(failedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalAttempts), 1, RoundingMode.HALF_UP);

        long overdueInstallments = installmentRepository.countByStatus(InstallmentStatus.OVERDUE);

        return RepaymentKpiDto.builder()
                .activeLoansCount(activeCount)
                .closedLoansCount(closedCount)
                .totalOutstanding(outstanding)
                .collectedThisMonth(collected)
                .failureRatePercent(failureRate)
                .overdueInstallmentsCount(overdueInstallments)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LoanSummaryDto> getAdminLoans(String adminEmail, LoanStatus status, Pageable pageable) {
        requireAdmin(adminEmail);
        Page<Loan> loans = status != null
                ? loanRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : loanRepository.findAllByOrderByCreatedAtDesc(pageable);
        return loans.map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public LoanDetailDto getLoanDetailForAdmin(Long loanId, String adminEmail) {
        requireAdmin(adminEmail);
        Loan loan = requireLoan(loanId);
        return toDetailDto(loan, true, true);
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getRepaymentHistoryForAdmin(Long loanId, String adminEmail) {
        requireAdmin(adminEmail);
        Loan loan = requireLoan(loanId);
        return historyService.getRepaymentHistory(loan.getLoanApplication().getId());
    }

    private Page<Loan> findAdvisorLoans(Long advisorId, LoanStatus status, boolean overdueOnly, Pageable pageable) {
        if (overdueOnly && status != null) {
            return loanRepository.findByAdvisorIdAndStatusWithOverdueInstallments(advisorId, status, pageable);
        }
        if (overdueOnly) {
            return loanRepository.findByAdvisorIdWithOverdueInstallments(advisorId, pageable);
        }
        if (status != null) {
            return loanRepository.findByAssignedAdvisorIdAndStatusOrderByCreatedAtDesc(advisorId, status, pageable);
        }
        return loanRepository.findByAssignedAdvisorIdOrderByCreatedAtDesc(advisorId, pageable);
    }

    private LoanSummaryDto toSummaryDto(Loan loan) {
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        Optional<Installment> nextInstallment = installmentRepository
                .findFirstByRepaymentPlan_Loan_IdAndStatusInOrderByDueDateAsc(loan.getId(), NEXT_INSTALLMENT_STATUSES);
        MandateInfo mandateInfo = resolveMandateInfo(loan.getId());
        long overdueCount = installmentRepository.countByLoanIdAndStatus(loan.getId(), InstallmentStatus.OVERDUE);
        long paidCount = plan != null
                ? installmentRepository.countByLoanIdAndStatus(loan.getId(), InstallmentStatus.PAID)
                : 0L;
        Integer installmentCount = plan != null ? plan.getInstallmentCount() : null;
        Integer remainingInstallments = installmentCount != null
                ? Math.max(0, installmentCount - (int) paidCount)
                : null;

        return LoanSummaryDto.builder()
                .id(loan.getId())
                .loanApplicationId(loan.getLoanApplication().getId())
                .reference(loan.getLoanApplication().getReference())
                .status(loan.getStatus().name())
                .principalAmount(loan.getPrincipalAmount())
                .remainingBalance(loan.getRemainingBalance())
                .monthlyPayment(plan != null ? plan.getMonthlyPayment() : null)
                .nextInstallmentDate(nextInstallment.map(Installment::getDueDate).orElse(null))
                .nextInstallmentAmount(nextInstallment.map(Installment::getAmountDue).orElse(null))
                .nextInstallmentStatus(nextInstallment.map(i -> i.getStatus().name()).orElse(null))
                .installmentCount(installmentCount)
                .remainingInstallmentsCount(remainingInstallments)
                .mandateStatus(mandateInfo.status())
                .borrowerName(displayName(loan.getBorrower()))
                .borrowerEmail(loan.getBorrower().getEmail())
                .overdueInstallmentsCount(overdueCount)
                .build();
    }

    private LoanDetailDto toDetailDto(Loan loan, boolean includeBorrowerInfo, boolean includeNestedData) {
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loan.getId()).orElse(null);
        Optional<Installment> nextInstallment = installmentRepository
                .findFirstByRepaymentPlan_Loan_IdAndStatusInOrderByDueDateAsc(loan.getId(), NEXT_INSTALLMENT_STATUSES);
        MandateInfo mandateInfo = resolveMandateInfo(loan.getId());
        long paidCount = plan != null
                ? installmentRepository.countByLoanIdAndStatus(loan.getId(), InstallmentStatus.PAID)
                : 0L;
        long overdueCount = installmentRepository.countByLoanIdAndStatus(loan.getId(), InstallmentStatus.OVERDUE);

        LoanDetailDto.LoanDetailDtoBuilder builder = LoanDetailDto.builder()
                .id(loan.getId())
                .loanApplicationId(loan.getLoanApplication().getId())
                .reference(loan.getLoanApplication().getReference())
                .status(loan.getStatus().name())
                .principalAmount(loan.getPrincipalAmount())
                .remainingBalance(loan.getRemainingBalance())
                .monthlyPayment(plan != null ? plan.getMonthlyPayment() : null)
                .durationMonths(loan.getDurationMonths())
                .annualRate(loan.getAnnualRate())
                .totalRepayable(plan != null ? plan.getTotalRepayable() : null)
                .installmentCount(plan != null ? plan.getInstallmentCount() : null)
                .paidInstallmentsCount(paidCount)
                .overdueInstallmentsCount(overdueCount)
                .nextInstallmentDate(nextInstallment.map(Installment::getDueDate).orElse(null))
                .nextInstallmentAmount(nextInstallment.map(Installment::getAmountDue).orElse(null))
                .mandateStatus(mandateInfo.status())
                .ibanMasked(mandateInfo.ibanMasked())
                .holderName(mandateInfo.holderName())
                .activatedAt(loan.getActivatedAt())
                .closedAt(loan.getClosedAt());

        if (includeBorrowerInfo) {
            builder.borrowerName(displayName(loan.getBorrower()))
                    .borrowerEmail(loan.getBorrower().getEmail());
        }
        if (loan.getAssignedAdvisor() != null) {
            builder.advisorName(displayName(loan.getAssignedAdvisor()));
        }
        if (includeNestedData) {
            builder.installments(loadInstallments(loan.getId()))
                    .transactions(loadTransactions(loan.getId()));
        }

        return builder.build();
    }

    private List<InstallmentDto> loadInstallments(Long loanId) {
        RepaymentPlan plan = repaymentPlanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de remboursement introuvable"));
        return installmentRepository.findByRepaymentPlanIdOrderBySequenceNumberAsc(plan.getId()).stream()
                .map(this::toInstallmentDto)
                .toList();
    }

    private List<PaymentTransactionDto> loadTransactions(Long loanId) {
        return paymentTransactionRepository.findByLoanIdOrderByAttemptedAtDesc(loanId).stream()
                .map(this::toTransactionDto)
                .toList();
    }

    private InstallmentDto toInstallmentDto(Installment installment) {
        return InstallmentDto.builder()
                .id(installment.getId())
                .sequenceNumber(installment.getSequenceNumber())
                .dueDate(installment.getDueDate())
                .amountDue(installment.getAmountDue())
                .principalPart(installment.getPrincipalPart())
                .interestPart(installment.getInterestPart())
                .remainingBalance(installment.getRemainingBalance())
                .status(installment.getStatus().name())
                .attemptCount(installment.getAttemptCount())
                .nextRetryDate(installment.getNextRetryDate())
                .build();
    }

    private PaymentTransactionDto toTransactionDto(PaymentTransaction transaction) {
        Installment installment = transaction.getInstallment();
        return PaymentTransactionDto.builder()
                .id(transaction.getId())
                .installmentId(installment.getId())
                .sequenceNumber(installment.getSequenceNumber())
                .attemptNumber(transaction.getAttemptNumber())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .failureReason(transaction.getFailureReason())
                .externalReference(transaction.getExternalReference())
                .attemptedAt(transaction.getAttemptedAt())
                .settledAt(transaction.getSettledAt())
                .build();
    }

    private MandateInfo resolveMandateInfo(Long loanId) {
        return mandateRepository.findByLoanId(loanId)
                .map(mandate -> new MandateInfo(
                        mandate.getStatus().name(),
                        mandate.getPaymentMethod().getIbanMasked(),
                        mandate.getPaymentMethod().getHolderName()
                ))
                .orElse(new MandateInfo("NONE", null, null));
    }

    private Loan requireLoan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Prêt introuvable"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private User requireAdvisor(String email) {
        User user = requireUser(email);
        if (!hasRole(user, ROLE_CONSEILLER) && !hasRole(user, ROLE_ADMIN)) {
            throw new ForbiddenOperationException("Accès réservé aux conseillers.");
        }
        return user;
    }

    private void requireAdmin(String email) {
        User user = requireUser(email);
        if (!hasRole(user, ROLE_ADMIN)) {
            throw new ForbiddenOperationException("Accès réservé aux administrateurs.");
        }
    }

    private void assertBorrowerAccess(Loan loan, String borrowerEmail) {
        if (!loan.getBorrower().getEmail().equalsIgnoreCase(borrowerEmail)) {
            throw new ForbiddenOperationException("Accès refusé à ce prêt.");
        }
    }

    private void assertAdvisorAccess(Loan loan, User advisor) {
        if (hasRole(advisor, ROLE_ADMIN)) {
            return;
        }
        if (loan.getAssignedAdvisor() == null
                || !loan.getAssignedAdvisor().getId().equals(advisor.getId())) {
            throw new ForbiddenOperationException("Accès refusé à ce prêt.");
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName::equals);
    }

    private static String displayName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private record MandateInfo(String status, String ibanMasked, String holderName) {
    }
}
