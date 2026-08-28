package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RepaymentKpiDto {

    long activeLoansCount;
    long closedLoansCount;
    BigDecimal totalOutstanding;
    BigDecimal collectedThisMonth;
    BigDecimal failureRatePercent;
    long overdueInstallmentsCount;
}
