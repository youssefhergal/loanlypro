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
public class LoanDocumentResponseDto {
    private Long id;
    private Long loanApplicationId;
    private LoanDocumentType documentType;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private Instant uploadedAt;
}
