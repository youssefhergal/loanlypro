package com.projetfilrouge.loanmanagement.security;

import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class SchedulerAccessService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final String internalToken;

    public SchedulerAccessService(
            @Value("${app.repayment.scheduler.internal-token:}") String internalToken
    ) {
        this.internalToken = internalToken == null ? "" : internalToken.trim();
    }

    public void ensureCanRunScheduler(String schedulerToken, Authentication authentication) {
        if (!internalToken.isEmpty() && internalToken.equals(schedulerToken)) {
            return;
        }
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(ROLE_ADMIN::equals);
            if (isAdmin) {
                return;
            }
        }
        throw new ForbiddenOperationException(
                "Accès refusé : rôle administrateur ou en-tête X-Scheduler-Token requis."
        );
    }
}
