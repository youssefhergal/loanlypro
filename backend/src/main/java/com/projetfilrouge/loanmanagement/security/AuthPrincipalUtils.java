package com.projetfilrouge.loanmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthPrincipalUtils {

    private AuthPrincipalUtils() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static String resolveEmail(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return normalizeEmail(userDetails.getUsername());
        }
        if (principal instanceof String email) {
            return normalizeEmail(email);
        }
        return normalizeEmail(authentication.getName());
    }
}
