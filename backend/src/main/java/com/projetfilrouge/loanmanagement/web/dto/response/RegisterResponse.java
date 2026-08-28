package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {

    private UserResponse user;
    private String message;
}
