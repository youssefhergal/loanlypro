package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MandateResponseDto {

    Long loanId;
    String mandateReference;
    String status;
    String ibanMasked;
    String holderName;
}
