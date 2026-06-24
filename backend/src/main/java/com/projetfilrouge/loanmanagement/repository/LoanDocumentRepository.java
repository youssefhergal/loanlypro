package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanDocument;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {
    List<LoanDocument> findByLoanApplicationIdOrderByUploadedAtDesc(Long loanApplicationId);

    Optional<LoanDocument> findByIdAndLoanApplicationId(Long id, Long loanApplicationId);

    long countByLoanApplicationIdAndDocumentType(Long loanApplicationId, LoanDocumentType documentType);

    /** US-6.1 — agrégation GED client (squelette pour ClientDocumentsService). */
    @Query("""
            SELECT d FROM LoanDocument d
            JOIN d.loanApplication la
            JOIN la.applicant u
            WHERE LOWER(u.email) = LOWER(:email)
            ORDER BY la.id DESC, d.uploadedAt DESC
            """)
    List<LoanDocument> findAllByApplicantEmail(@Param("email") String email);
}
