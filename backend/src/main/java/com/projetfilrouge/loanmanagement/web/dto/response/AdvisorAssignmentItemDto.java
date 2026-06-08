package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorAssignmentItemDto {
    private Long loanId;
    private String reference;
    private Long advisorId;
    private String advisorName;
}
