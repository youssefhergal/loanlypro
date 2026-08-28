package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDocumentReviewResponseDto {
    private LoanDocumentType documentType;
    /** pending_review | validated | rejected | missing_upload */
    private String status;
    private String comment;
    private Instant updatedAt;
}
