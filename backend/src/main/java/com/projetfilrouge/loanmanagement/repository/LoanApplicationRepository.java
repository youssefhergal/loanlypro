package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByReference(String reference);

    List<LoanApplication> findByStatus(LoanApplicationStatus status);

    List<LoanApplication> findByStatusOrderBySubmittedAtDesc(LoanApplicationStatus status);

    List<LoanApplication> findByApplicantEmail(String email);

    List<LoanApplication> findByAssignedAdvisorId(Long advisorId);

    boolean existsByReference(String reference);
}