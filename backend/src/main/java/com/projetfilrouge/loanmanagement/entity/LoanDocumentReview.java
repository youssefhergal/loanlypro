package com.projetfilrouge.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "loan_document_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_loan_document_review_type",
                columnNames = {"loan_application_id", "document_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDocumentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private LoanDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private LoanDocumentReviewStatus reviewStatus;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
