package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectLoanRequestDto {

    @NotBlank(message = "Le motif du refus est obligatoire")
    @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
    private String comment;
}
