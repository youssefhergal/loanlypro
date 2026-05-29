package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<LoanApplication> findByStatus(LoanApplicationStatus status, Pageable pageable);

    Page<LoanApplication> findByApplicantEmail(String email, Pageable pageable);

    Page<LoanApplication> findByApplicantEmailAndStatus(String email, LoanApplicationStatus status, Pageable pageable);

    Page<LoanApplication> findByAssignedAdvisorId(Long advisorId, Pageable pageable);

    Page<LoanApplication> findByAssignedAdvisorIdAndStatus(Long advisorId, LoanApplicationStatus status, Pageable pageable);

    boolean existsByReference(String reference);
}