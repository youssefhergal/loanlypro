package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanDocumentReview;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanDocumentReviewRepository extends JpaRepository<LoanDocumentReview, Long> {

    List<LoanDocumentReview> findByLoanApplicationIdOrderByDocumentTypeAsc(Long loanApplicationId);

    Optional<LoanDocumentReview> findByLoanApplicationIdAndDocumentType(
            Long loanApplicationId,
            LoanDocumentType documentType
    );
}
