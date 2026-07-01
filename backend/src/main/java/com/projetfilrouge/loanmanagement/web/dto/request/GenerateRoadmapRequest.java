package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GenerateRoadmapRequest {

    @NotBlank
    @Size(min = 5, max = 500)
    private String objective;
}
