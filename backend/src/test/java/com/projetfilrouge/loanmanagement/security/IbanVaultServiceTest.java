package com.projetfilrouge.loanmanagement.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IbanVaultServiceTest {

    private final IbanVaultService vault = new IbanVaultService("test-vault-key");

    @Test
    void tokenizeAndResolve_roundTrip() {
        String iban = "FR1420041010050500013M02606";

        String token = vault.tokenize(iban);

        assertThat(token).startsWith("v1:");
        assertThat(vault.resolve(token)).isEqualTo(iban);
    }

    @Test
    void resolve_legacyPlainToken_returnsAsIs() {
        String legacy = "FR1420041010050500013M02606";

        assertThat(vault.resolve(legacy)).isEqualTo(legacy);
    }
}
