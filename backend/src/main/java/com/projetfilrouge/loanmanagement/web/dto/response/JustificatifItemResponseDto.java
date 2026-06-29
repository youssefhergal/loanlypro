package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.DocumentValidationStatus;
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
public class JustificatifItemResponseDto {

    private Long documentId;
    private LoanDocumentType documentType;
    private String documentTypeLabel;
    private String fileName;
    private Instant uploadedAt;
    private DocumentValidationStatus validationStatus;
    private String rejectionReason;
    private boolean downloadable;
}
