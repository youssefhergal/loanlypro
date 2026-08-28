package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectOfferRequestDto {

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    private String comment;
}
