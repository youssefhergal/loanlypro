package com.projetfilrouge.loanmanagement.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le prénom est requis")
    @JsonProperty("firstname")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @JsonProperty("lastname")
    private String lastName;

    @NotBlank(message = "Le mot de passe est requis")
    private String password;
}
