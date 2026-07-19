package biz.thonbecker.personal.user.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import biz.thonbecker.personal.user.api.UserLoggedOutEvent;
import biz.thonbecker.personal.user.platform.persistence.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class MagicLinkAuthenticationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private UserService userService;
    private ApplicationEventPublisher eventPublisher;
    private MagicLinkAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        userService = mock(UserService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        authenticationService = new MagicLinkAuthenticationService(jdbcTemplate, userService, eventPublisher);
    }

    @Test
    void rejectsMissingSessionTokens() {
        assertThat(authenticationService.resolveUserId(null)).isEmpty();
        assertThat(authenticationService.resolveUserId(" ")).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void resolvesActiveSessionByHashedToken() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of("user-1"));

        assertThat(authenticationService.resolveUserId("session-token")).contains("user-1");
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString());
    }

    @Test
    void rejectsLoginRequestsAfterRateLimit() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenReturn(5);

        authenticationService.requestLoginLink("person@example.com", "https://app.example.com", "127.0.0.1");

        verify(userService, never()).registerUser(anyString(), anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsMalformedEmailAddresses() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        authenticationService.requestLoginLink("not-an-email", "https://app.example.com", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void logsOutAndRevokesAnActiveSession() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of("user-1"));

        authenticationService.logout("session-token");

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
        verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
    }
}
