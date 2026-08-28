package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.Installment;
import com.projetfilrouge.loanmanagement.entity.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByRepaymentPlanIdOrderBySequenceNumberAsc(Long repaymentPlanId);

    @Query("""
            SELECT i FROM Installment i
            JOIN FETCH i.repaymentPlan p
            JOIN FETCH p.loan l
            JOIN FETCH l.loanApplication
            WHERE i.status = com.projetfilrouge.loanmanagement.entity.InstallmentStatus.UPCOMING
              AND i.dueDate <= :processingDate
              AND l.status IN (
                  com.projetfilrouge.loanmanagement.entity.LoanStatus.ACTIVE,
                  com.projetfilrouge.loanmanagement.entity.LoanStatus.PENDING_MANDATE
              )
            """)
    List<Installment> findDueInstallments(@Param("processingDate") LocalDate processingDate);

    @Query("""
            SELECT i FROM Installment i
            JOIN FETCH i.repaymentPlan p
            JOIN FETCH p.loan l
            JOIN FETCH l.loanApplication
            WHERE i.status = com.projetfilrouge.loanmanagement.entity.InstallmentStatus.FAILED
              AND i.nextRetryDate = :processingDate
              AND i.attemptCount < :maxAttempts
              AND l.status = com.projetfilrouge.loanmanagement.entity.LoanStatus.ACTIVE
            """)
    List<Installment> findRetryInstallments(
            @Param("processingDate") LocalDate processingDate,
            @Param("maxAttempts") int maxAttempts
    );

    @Query("""
            SELECT COUNT(i) FROM Installment i
            WHERE i.repaymentPlan.loan.id = :loanId
              AND i.status = :status
            """)
    long countByLoanIdAndStatus(@Param("loanId") Long loanId, @Param("status") InstallmentStatus status);

    long countByStatus(InstallmentStatus status);

    Optional<Installment> findFirstByRepaymentPlan_Loan_IdAndStatusInOrderByDueDateAsc(
            Long loanId,
            Collection<InstallmentStatus> statuses
    );
}