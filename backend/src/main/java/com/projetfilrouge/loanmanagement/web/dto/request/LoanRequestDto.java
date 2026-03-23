package com.projetfilrouge.loanmanagement.web.dto.request;

import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestDto {

    @NotNull(message = "Le montant demandé est obligatoire")
    @DecimalMin(value = "500.0", message = "Le montant minimum est de 500 €")
    @DecimalMax(value = "100000.0", message = "Le montant maximum est de 100 000 €")
    private BigDecimal requestedAmount;

    @NotNull(message = "La durée est obligatoire")
    @Min(value = 6, message = "La durée minimum est de 6 mois")
    @Max(value = 120, message = "La durée maximum est de 120 mois")
    private Integer requestedDurationMonths;

    @NotBlank(message = "Le motif du prêt est obligatoire")
    @Size(max = 255, message = "Le motif ne doit pas dépasser 255 caractères")
    private String purpose;

    @NotNull(message = "Le revenu mensuel est obligatoire")
    @DecimalMin(value = "0.0", message = "Le revenu ne peut pas être négatif")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Le statut professionnel est obligatoire")
    private EmploymentStatus employmentStatus;
}