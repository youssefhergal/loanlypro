package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.entity.MandateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectDebitMandateRepository extends JpaRepository<DirectDebitMandate, Long> {

    Optional<DirectDebitMandate> findByLoanId(Long loanId);

    Optional<DirectDebitMandate> findByLoanIdAndStatus(Long loanId, MandateStatus status);
}
