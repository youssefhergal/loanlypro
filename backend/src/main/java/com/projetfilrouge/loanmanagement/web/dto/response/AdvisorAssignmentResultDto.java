package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorAssignmentResultDto {
    private int assignedCount;
    private int unassignedRemaining;
    private int advisorsAvailable;
    private String message;
    private List<AdvisorAssignmentItemDto> assignments;
}
