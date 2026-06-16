package com.projetfilrouge.loanmanagement.security;

import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulerAccessServiceTest {

    private final SchedulerAccessService service = new SchedulerAccessService("dev-scheduler-token");

    @Test
    void ensureCanRunScheduler_allowsValidToken() {
        assertThatCode(() -> service.ensureCanRunScheduler("dev-scheduler-token", null))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureCanRunScheduler_allowsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThatCode(() -> service.ensureCanRunScheduler(null, auth))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureCanRunScheduler_rejectsClient() {
        var auth = new UsernamePasswordAuthenticationToken(
                "client@test.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );

        assertThatThrownBy(() -> service.ensureCanRunScheduler(null, auth))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
