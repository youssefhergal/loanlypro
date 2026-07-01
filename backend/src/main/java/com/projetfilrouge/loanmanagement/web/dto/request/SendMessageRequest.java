package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(max = 4000, message = "Le message ne doit pas dépasser 4000 caractères")
    private String content;
}
