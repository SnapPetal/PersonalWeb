package biz.thonbecker.personal.user.platform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import biz.thonbecker.personal.user.platform.persistence.UserLoginTokenRepository;
import biz.thonbecker.personal.user.platform.persistence.UserSessionRepository;
import org.junit.jupiter.api.Test;

class AuthenticationCleanupServiceTest {

    @Test
    void deletesExpiredTokensAndSessions() {
        final var loginTokenRepository = mock(UserLoginTokenRepository.class);
        final var sessionRepository = mock(UserSessionRepository.class);
        final var cleanupService = new AuthenticationCleanupService(loginTokenRepository, sessionRepository);

        cleanupService.cleanupExpiredAuthenticationData();

        verify(loginTokenRepository, times(1)).deleteByExpiresAtBeforeOrUsedAtBefore(any(), any());
        verify(sessionRepository, times(1)).deleteByExpiresAtBeforeOrRevokedAtBefore(any(), any());
    }
}
