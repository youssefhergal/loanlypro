package com.projetfilrouge.loanmanagement.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Coffre-fort IBAN simulé : stockage chiffré AES-GCM en base (préfixe {@code v1:}).
 * Les anciens tokens en clair restent lisibles pour la migration dev.
 */
@Service
public class IbanVaultService {

    private static final String TOKEN_PREFIX = "v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public IbanVaultService(@Value("${app.payment.iban-vault-key:}") String vaultKey) {
        this.encryptionKey = deriveKey(vaultKey);
    }

    public String tokenize(String normalizedIban) {
        if (normalizedIban == null || normalizedIban.isBlank()) {
            throw new IllegalArgumentException("IBAN vide.");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(normalizedIban.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return TOKEN_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de chiffrer l'IBAN.", ex);
        }
    }

    public String resolve(String storedToken) {
        if (storedToken == null || storedToken.isBlank()) {
            throw new IllegalArgumentException("Token IBAN manquant.");
        }
        if (!storedToken.startsWith(TOKEN_PREFIX)) {
            return storedToken;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedToken.substring(TOKEN_PREFIX.length()));
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(payload, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de déchiffrer l'IBAN.", ex);
        }
    }

    private static byte[] deriveKey(String configuredKey) {
        String source = configuredKey == null || configuredKey.isBlank()
                ? "loanlyfans-dev-iban-vault-key-change-me"
                : configuredKey;
        try {
            return MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Clé vault IBAN invalide.", ex);
        }
    }
}
