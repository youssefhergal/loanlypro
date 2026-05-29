package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationEventRepository extends JpaRepository<LoanApplicationEvent, Long> {

    List<LoanApplicationEvent> findByLoanApplicationIdOrderByOccurredAtAsc(Long loanApplicationId);

    boolean existsByLoanApplicationId(Long loanApplicationId);

    void deleteByLoanApplicationId(Long loanApplicationId);
}
