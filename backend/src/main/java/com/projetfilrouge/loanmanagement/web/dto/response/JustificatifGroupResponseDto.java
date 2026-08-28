package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JustificatifGroupResponseDto {

    private Long loanApplicationId;
    private String loanReference;
    private String loanStatus;
    private Instant submittedAt;
    private Instant updatedAt;
    private List<JustificatifItemResponseDto> documents;
}
