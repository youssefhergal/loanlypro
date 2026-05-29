package com.projetfilrouge.loanmanagement.web.dto.request;

import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectDocumentRequestDto {

    @NotNull(message = "Le type de document est obligatoire")
    private LoanDocumentType documentType;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
    private String comment;
}
