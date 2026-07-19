package biz.thonbecker.personal.user.platform;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthenticationCleanupServiceTest {

    @Test
    void deletesExpiredTokensAndSessions() {
        final var jdbcTemplate = mock(JdbcTemplate.class);
        final var cleanupService = new AuthenticationCleanupService(jdbcTemplate);

        cleanupService.cleanupExpiredAuthenticationData();

        verify(jdbcTemplate, times(2)).update(anyString());
    }
}
