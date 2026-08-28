package com.projetfilrouge.loanmanagement.web.dto.request;



import com.projetfilrouge.loanmanagement.entity.EmploymentStatus;

import com.projetfilrouge.loanmanagement.entity.LoanPurpose;

import jakarta.validation.constraints.*;

import lombok.*;



import java.math.BigDecimal;
import java.time.LocalDate;



@Data

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class LoanRequestDto {



    @NotBlank(message = "L'intitulé est obligatoire")

    @Size(max = 120, message = "L'intitulé ne doit pas dépasser 120 caractères")

    private String title;



    @NotNull(message = "La finalité du prêt est obligatoire")

    private LoanPurpose loanPurpose;



    @NotNull(message = "Le montant demandé est obligatoire")

    @DecimalMin(value = "1000.0", message = "Le montant minimum est de 1 000 €")

    @DecimalMax(value = "200000.0", message = "Le montant maximum est de 200 000 €")

    private BigDecimal requestedAmount;



    @NotNull(message = "La durée est obligatoire")

    @Min(value = 12, message = "La durée minimum est de 12 mois")

    @Max(value = 240, message = "La durée maximum est de 240 mois")

    private Integer requestedDurationMonths;



    @Size(max = 500, message = "Le commentaire ne doit pas dépasser 500 caractères")

    private String comment;



    @NotNull(message = "Le revenu mensuel est obligatoire")

    @DecimalMin(value = "0.0", message = "Le revenu ne peut pas être négatif")

    private BigDecimal monthlyIncome;



    @NotNull(message = "Le statut professionnel est obligatoire")

    private EmploymentStatus employmentStatus;



    @DecimalMin(value = "0.0", message = "Les revenus complémentaires ne peuvent pas être négatifs")

    private BigDecimal additionalIncome;



    @Size(max = 120, message = "Le nom de l'employeur ne doit pas dépasser 120 caractères")

    private String employerName;

    @Size(max = 120, message = "Le poste ne doit pas dépasser 120 caractères")
    private String jobTitle;

    @Size(max = 40, message = "Le secteur ne doit pas dépasser 40 caractères")
    private String employerSector;

    private LocalDate hireDate;

    @Min(value = 0, message = "L'ancienneté ne peut pas être négative")

    private Integer seniorityMonths;



    @DecimalMin(value = "0.0", message = "Le loyer ne peut pas être négatif")

    private BigDecimal monthlyRent;



    @DecimalMin(value = "0.0", message = "Les crédits en cours ne peuvent pas être négatifs")

    private BigDecimal monthlyLoanPayments;



    @DecimalMin(value = "0.0", message = "La pension alimentaire ne peut pas être négative")

    private BigDecimal monthlyAlimony;



    @DecimalMin(value = "0.0", message = "Les autres charges ne peuvent pas être négatives")

    private BigDecimal monthlyOtherCharges;

}

