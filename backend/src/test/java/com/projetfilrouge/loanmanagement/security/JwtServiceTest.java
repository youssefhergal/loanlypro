package com.projetfilrouge.loanmanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "testSecretKeyForUnitTestsMinimum32Chars!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_validateToken_andGetEmail_succeedForAuthenticatedUser() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "client@test.com",
                null
        );

        String token = jwtService.generateToken(authentication);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getEmailFromToken(token)).isEqualTo("client@test.com");
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        assertThat(jwtService.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void validateToken_returnsFalseForTamperedToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "client@test.com",
                null
        );
        String token = jwtService.generateToken(authentication);
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
    }
}
