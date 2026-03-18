package com.projetfilrouge.loanmanagement.web.dto.request;

import com.projetfilrouge.loanmanagement.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

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
    private String firstname;

    @NotBlank(message = "Le nom est requis")
    private String lastname;

    @NotBlank(message = "Le mot de passe est requis")
    private String password;
}
