package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepaymentPlanRepository extends JpaRepository<RepaymentPlan, Long> {

    Optional<RepaymentPlan> findByLoanId(Long loanId);
}
