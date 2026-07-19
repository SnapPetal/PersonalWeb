package biz.thonbecker.personal.user.platform.web;

import biz.thonbecker.personal.user.api.UserSessionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class MagicLinkAuthenticationFilter extends OncePerRequestFilter {

    private final UserSessionResolver sessionResolver;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null && request.getCookies() != null) {
            java.util.Arrays.stream(request.getCookies())
                    .filter(cookie -> UserSessionResolver.SESSION_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .flatMap(sessionResolver::resolveUserId)
                    .ifPresent(userId -> SecurityContextHolder.getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of())));
        }
        filterChain.doFilter(request, response);
    }
}
