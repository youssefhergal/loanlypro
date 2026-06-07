package com.projetfilrouge.loanmanagement.web.dto.request;

import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateDocumentRequestDto {

    @NotNull(message = "Le type de document est obligatoire")
    private LoanDocumentType documentType;
}
