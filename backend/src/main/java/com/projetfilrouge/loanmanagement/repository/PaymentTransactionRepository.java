package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.PaymentTransaction;
import com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByInstallmentIdAndAttemptNumber(Long installmentId, Integer attemptNumber);

    @Query("""
            SELECT pt FROM PaymentTransaction pt
            JOIN FETCH pt.installment i
            JOIN FETCH i.repaymentPlan p
            WHERE p.loan.id = :loanId
            ORDER BY pt.attemptedAt DESC, pt.id DESC
            """)
    List<PaymentTransaction> findByLoanIdOrderByAttemptedAtDesc(@Param("loanId") Long loanId);

    long countByStatus(PaymentTransactionStatus status);

    @Query("""
            SELECT COALESCE(SUM(pt.amount), 0)
            FROM PaymentTransaction pt
            WHERE pt.status = com.projetfilrouge.loanmanagement.entity.PaymentTransactionStatus.SUCCESS
              AND pt.settledAt >= :start
              AND pt.settledAt < :end
            """)
    BigDecimal sumSuccessfulAmountBetween(@Param("start") Instant start, @Param("end") Instant end);
}
