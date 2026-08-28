package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProposeOfferRequestDto {

    @NotNull
    @DecimalMin("1000")
    private BigDecimal approvedAmount;

    @NotNull
    @Min(12)
    private Integer approvedDurationMonths;

    @NotNull
    @DecimalMin("0")
    private BigDecimal interestRate;

    @Size(max = 500)
    private String clientMessage;
}
