package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepaymentSchedulerRunResultDto {

    int processedCount;
    int successCount;
    int failedCount;
    int blockedCount;
    int skippedCount;
    int overdueCount;
}
