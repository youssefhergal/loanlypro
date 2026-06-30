package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String role;
}
