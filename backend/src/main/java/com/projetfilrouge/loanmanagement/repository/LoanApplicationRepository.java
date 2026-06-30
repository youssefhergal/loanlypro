package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long>, JpaSpecificationExecutor<LoanApplication> {

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

    @Query("""
            SELECT l FROM LoanApplication l
            WHERE l.assignedAdvisor.id = :advisorId
               OR (l.assignedAdvisor IS NULL AND l.status = com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.SUBMITTED)
            """)
    Page<LoanApplication> findVisibleToAdvisor(@Param("advisorId") Long advisorId, Pageable pageable);

    @Query("""
            SELECT l FROM LoanApplication l
            WHERE (l.assignedAdvisor.id = :advisorId
               OR (l.assignedAdvisor IS NULL AND l.status = com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.SUBMITTED))
              AND l.status = :status
            """)
    Page<LoanApplication> findVisibleToAdvisorAndStatus(
            @Param("advisorId") Long advisorId,
            @Param("status") LoanApplicationStatus status,
            Pageable pageable
    );

    boolean existsByReference(String reference);

    @Query("""
            SELECT l FROM LoanApplication l
            WHERE l.status = com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.SUBMITTED
              AND l.assignedAdvisor IS NULL
            ORDER BY l.submittedAt ASC, l.createdAt ASC
            """)
    List<LoanApplication> findUnassignedSubmittedOrderBySubmittedAtAsc();

    @Query("""
            SELECT COUNT(l) FROM LoanApplication l
            WHERE l.assignedAdvisor.id = :advisorId
              AND l.status IN (
                com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.SUBMITTED,
                com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.UNDER_REVIEW,
                com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.OFFER_PENDING
              )
            """)
    long countActiveAssignments(@Param("advisorId") Long advisorId);

    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.status = com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus.APPROVED
              AND NOT EXISTS (
                SELECT 1 FROM Loan l WHERE l.loanApplication.id = la.id
              )
            ORDER BY la.decidedAt ASC, la.id ASC
            """)
    List<LoanApplication> findApprovedWithoutRepaymentLoan();

    @Query("""
            SELECT la FROM LoanApplication la
            JOIN FETCH la.applicant
            LEFT JOIN FETCH la.assignedAdvisor
            WHERE la.id = :id
            """)
    Optional<LoanApplication> findByIdWithApplicantAndAdvisor(@Param("id") Long id);

    boolean existsByApplicantEmailAndAssignedAdvisorId(String applicantEmail, Long advisorId);

    @Query("""
            SELECT DISTINCT la.applicant FROM LoanApplication la
            WHERE la.assignedAdvisor.id = :advisorId
            """)
    List<com.projetfilrouge.loanmanagement.entity.User> findClientsByAdvisorId(@Param("advisorId") Long advisorId);

    @Query("""
            SELECT DISTINCT la.assignedAdvisor FROM LoanApplication la
            WHERE la.applicant.email = :email
              AND la.assignedAdvisor IS NOT NULL
            """)
    List<com.projetfilrouge.loanmanagement.entity.User> findAdvisorsByClientEmail(@Param("email") String email);
}