package biz.thonbecker.personal.user.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biz.thonbecker.personal.user.api.UserSessionResolver;
import biz.thonbecker.personal.user.platform.web.MagicLinkAuthenticationFilter;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class MagicLinkAuthenticationFilterTest {

    private final UserSessionResolver sessionResolver = mock(UserSessionResolver.class);
    private final MagicLinkAuthenticationFilter filter = new MagicLinkAuthenticationFilter(sessionResolver);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesRequestFromMagicLinkCookie() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(UserSessionResolver.SESSION_COOKIE_NAME, "session-token"));
        final var response = new MockHttpServletResponse();
        final var chain = mock(FilterChain.class);
        when(sessionResolver.resolveUserId("session-token")).thenReturn(Optional.of("user-1"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user-1");
        verify(chain).doFilter(request, response);
    }

    @Test
    void leavesRequestUnauthenticatedForInvalidCookie() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(UserSessionResolver.SESSION_COOKIE_NAME, "expired"));
        final var response = new MockHttpServletResponse();
        final var chain = mock(FilterChain.class);
        when(sessionResolver.resolveUserId("expired")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
