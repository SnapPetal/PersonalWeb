package biz.thonbecker.personal.user.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import biz.thonbecker.personal.user.api.UserLoggedOutEvent;
import biz.thonbecker.personal.user.platform.persistence.UserLoginTokenRepository;
import biz.thonbecker.personal.user.platform.persistence.UserService;
import biz.thonbecker.personal.user.platform.persistence.UserSessionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class MagicLinkAuthenticationServiceTest {

    private UserLoginTokenRepository loginTokenRepository;
    private UserSessionRepository sessionRepository;
    private UserService userService;
    private ApplicationEventPublisher eventPublisher;
    private MagicLinkAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        loginTokenRepository = mock(UserLoginTokenRepository.class);
        sessionRepository = mock(UserSessionRepository.class);
        userService = mock(UserService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        authenticationService = new MagicLinkAuthenticationService(
                loginTokenRepository, sessionRepository, userService, eventPublisher);
    }

    @Test
    void rejectsMissingSessionTokens() {
        assertThat(authenticationService.resolveUserId(null)).isEmpty();
        assertThat(authenticationService.resolveUserId(" ")).isEmpty();
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void resolvesActiveSessionByHashedToken() {
        final var session = new biz.thonbecker.personal.user.platform.persistence.UserSessionEntity();
        session.setUserId("user-1");
        when(sessionRepository.findBySessionHashAndRevokedAtIsNullAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(session));

        assertThat(authenticationService.resolveUserId("session-token")).contains("user-1");
        verify(sessionRepository).findBySessionHashAndRevokedAtIsNullAndExpiresAtAfter(anyString(), any(Instant.class));
    }

    @Test
    void rejectsLoginRequestsAfterRateLimit() {
        when(loginTokenRepository.countRecentRequests(anyString(), anyString(), any(Instant.class)))
                .thenReturn(5L);

        authenticationService.requestLoginLink("person@example.com", "https://app.example.com", "127.0.0.1");

        verify(userService, never()).registerUser(anyString(), anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsMalformedEmailAddresses() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        authenticationService.requestLoginLink("not-an-email", "https://app.example.com", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(loginTokenRepository);
    }

    @Test
    void logsOutAndRevokesAnActiveSession() {
        final var session = new biz.thonbecker.personal.user.platform.persistence.UserSessionEntity();
        session.setUserId("user-1");
        when(sessionRepository.findBySessionHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(session));

        authenticationService.logout("session-token");

        verify(sessionRepository).save(session);
        verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
    }
}
