package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanApplicationId(Long loanApplicationId);

    boolean existsByLoanApplicationId(Long loanApplicationId);

    List<Loan> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    Page<Loan> findByAssignedAdvisorIdOrderByCreatedAtDesc(Long advisorId, Pageable pageable);

    Page<Loan> findByAssignedAdvisorIdAndStatusOrderByCreatedAtDesc(
            Long advisorId,
            LoanStatus status,
            Pageable pageable
    );

    Page<Loan> findByStatusOrderByCreatedAtDesc(LoanStatus status, Pageable pageable);

    Page<Loan> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(LoanStatus status);

    @Query("""
            SELECT COALESCE(SUM(l.remainingBalance), 0)
            FROM Loan l
            WHERE l.status IN :statuses
            """)
    BigDecimal sumRemainingBalanceByStatusIn(@Param("statuses") Collection<LoanStatus> statuses);

    @Query("""
            SELECT DISTINCT l FROM Loan l
            JOIN RepaymentPlan p ON p.loan = l
            JOIN p.installments i
            WHERE l.assignedAdvisor.id = :advisorId
              AND i.status = com.projetfilrouge.loanmanagement.entity.InstallmentStatus.OVERDUE
            ORDER BY l.createdAt DESC
            """)
    Page<Loan> findByAdvisorIdWithOverdueInstallments(@Param("advisorId") Long advisorId, Pageable pageable);

    @Query("""
            SELECT DISTINCT l FROM Loan l
            JOIN RepaymentPlan p ON p.loan = l
            JOIN p.installments i
            WHERE l.assignedAdvisor.id = :advisorId
              AND l.status = :status
              AND i.status = com.projetfilrouge.loanmanagement.entity.InstallmentStatus.OVERDUE
            ORDER BY l.createdAt DESC
            """)
    Page<Loan> findByAdvisorIdAndStatusWithOverdueInstallments(
            @Param("advisorId") Long advisorId,
            @Param("status") LoanStatus status,
            Pageable pageable
    );
}
