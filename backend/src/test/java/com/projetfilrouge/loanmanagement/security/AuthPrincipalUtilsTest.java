package com.projetfilrouge.loanmanagement.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPrincipalUtilsTest {

    @Test
    void resolveEmail_usesUserDetailsUsername() {
        var principal = User.withUsername("Client.Seed@LoanlyPro.fr")
                .password("n/a")
                .authorities(List.of())
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        assertThat(AuthPrincipalUtils.resolveEmail(authentication))
                .isEqualTo("client.seed@loanlypro.fr");
    }

    @Test
    void resolveEmail_usesStringPrincipal() {
        var authentication = new UsernamePasswordAuthenticationToken("Advisor@test.com", null, List.of());

        assertThat(AuthPrincipalUtils.resolveEmail(authentication))
                .isEqualTo("advisor@test.com");
    }
}
