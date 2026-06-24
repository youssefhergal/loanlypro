package com.projetfilrouge.loanmanagement.web.dto.response;

import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditDocumentResponseDto {

    private IssuedDocumentType documentType;
    private String title;
    private Long loanApplicationId;
    private Long loanId;
    private String reference;
    private Instant issuedAt;
    private boolean available;
    private String unavailableReason;
}
