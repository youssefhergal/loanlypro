package com.projetfilrouge.loanmanagement.web.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Données de mise à jour pour une demande au statut SUBMITTED.
 * Tous les champs sont optionnels: uniquement ceux non-nuls seront appliqués.
 */
@Data
public class LoanSubmittedUpdateDto {
    private Long assignedAdvisorId; // id du conseiller à assigner
    private BigDecimal approvedAmount; // montant approuvé proposé
    private Integer approvedDurationMonths; // durée approuvée proposée
    private BigDecimal interestRate; // taux proposé
}
