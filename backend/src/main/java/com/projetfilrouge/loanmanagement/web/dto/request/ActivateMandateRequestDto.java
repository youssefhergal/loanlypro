package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateMandateRequestDto {

    @NotBlank(message = "L'IBAN est obligatoire")
    @Size(min = 15, max = 34, message = "L'IBAN n'est pas valide")
    private String iban;

    @NotBlank(message = "Le titulaire du compte est obligatoire")
    @Size(max = 120, message = "Le titulaire ne doit pas dépasser 120 caractères")
    private String holderName;
}
