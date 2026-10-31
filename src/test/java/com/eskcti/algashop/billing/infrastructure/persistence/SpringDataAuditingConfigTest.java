package com.eskcti.algashop.billing.infrastructure.persistence;

import com.eskcti.algashop.billing.application.security.SecurityChecks;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringDataAuditingConfigTest {

    @Test
    void shouldProvideCurrentDateTimeTruncatedToMillis() {
        DateTimeProvider dateTimeProvider = new SpringDataAuditingConfig().auditingDateTimeProvider();

        Optional<TemporalAccessor> now = dateTimeProvider.getNow();

        assertThat(now).isPresent();
        assertThat(now.get()).isInstanceOf(OffsetDateTime.class);
        OffsetDateTime value = OffsetDateTime.from(now.get());
        assertThat(value).isCloseToUtcNow(within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldProvideCurrentAuditorWhenUserIsAuthenticated() {
        SecurityChecks securityCheck = mock(SecurityChecks.class);
        UUID userId = UUID.randomUUID();
        when(securityCheck.isAuthenticated()).thenReturn(true);
        when(securityCheck.isMachineAuthenticated()).thenReturn(false);
        when(securityCheck.getAuthenticatedUserId()).thenReturn(userId);

        AuditorAware<UUID> auditorAware = new SpringDataAuditingConfig().auditorProvider(securityCheck);

        assertThat(auditorAware.getCurrentAuditor()).contains(userId);
    }

    @Test
    void shouldProvideNoAuditorWhenNotAuthenticated() {
        SecurityChecks securityCheck = mock(SecurityChecks.class);
        when(securityCheck.isAuthenticated()).thenReturn(false);

        AuditorAware<UUID> auditorAware = new SpringDataAuditingConfig().auditorProvider(securityCheck);

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void shouldProvideNoAuditorWhenMachineAuthenticated() {
        SecurityChecks securityCheck = mock(SecurityChecks.class);
        when(securityCheck.isAuthenticated()).thenReturn(true);
        when(securityCheck.isMachineAuthenticated()).thenReturn(true);

        AuditorAware<UUID> auditorAware = new SpringDataAuditingConfig().auditorProvider(securityCheck);

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
