package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.IssuedDocument;
import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssuedDocumentRepository extends JpaRepository<IssuedDocument, Long> {

    List<IssuedDocument> findByLoanApplication_Applicant_EmailOrderByIssuedAtDesc(String email);

    boolean existsByLoanApplication_IdAndDocumentType(Long loanApplicationId, IssuedDocumentType documentType);
}
