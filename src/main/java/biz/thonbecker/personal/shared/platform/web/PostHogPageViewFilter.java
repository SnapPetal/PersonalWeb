package biz.thonbecker.personal.shared.platform.web;

import static java.util.Objects.nonNull;

import biz.thonbecker.personal.shared.platform.configuration.PostHogProperties;
import biz.thonbecker.personal.shared.platform.service.PostHogAnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
class PostHogPageViewFilter extends OncePerRequestFilter {

    private static final String DISTINCT_ID_COOKIE_NAME = "posthog_distinct_id";

    private final PostHogAnalyticsService postHogAnalyticsService;
    private final PostHogProperties postHogProperties;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final var distinctId = resolveDistinctId(request, response);
        filterChain.doFilter(request, response);

        if (!shouldCapture(request, response)) {
            return;
        }

        postHogAnalyticsService.capture(distinctId, "$pageview", pageViewProperties(request, response));
    }

    private boolean shouldCapture(final HttpServletRequest request, final HttpServletResponse response) {
        if (!postHogProperties.isConfigured()) {
            return false;
        }

        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        if (response.getStatus() != HttpServletResponse.SC_OK) {
            return false;
        }

        final var requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api")
                || requestUri.startsWith("/actuator")
                || requestUri.startsWith("/css")
                || requestUri.startsWith("/js")
                || requestUri.startsWith("/webjars")
                || requestUri.startsWith("/images")
                || requestUri.startsWith("/favicon.ico")
                || requestUri.startsWith("/error")
                || requestUri.contains("/fragments/")
                || requestUri.contains("/htmx/")) {
            return false;
        }

        if (nonNull(request.getHeader("HX-Request"))) {
            return false;
        }

        final var contentType = response.getContentType();
        return nonNull(contentType) && contentType.startsWith(MediaType.TEXT_HTML_VALUE);
    }

    private String resolveDistinctId(final HttpServletRequest request, final HttpServletResponse response) {
        final Principal principal = request.getUserPrincipal();
        if (nonNull(principal)) {
            return principal.getName();
        }

        final var cookie = findCookie(request, DISTINCT_ID_COOKIE_NAME);
        if (nonNull(cookie) && nonNull(cookie.getValue()) && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        final var anonymousId = UUID.randomUUID().toString();
        final var distinctIdCookie = new Cookie(DISTINCT_ID_COOKIE_NAME, anonymousId);
        distinctIdCookie.setHttpOnly(true);
        distinctIdCookie.setPath("/");
        distinctIdCookie.setMaxAge(60 * 60 * 24 * 365);
        response.addCookie(distinctIdCookie);
        return anonymousId;
    }

    private Cookie findCookie(final HttpServletRequest request, final String cookieName) {
        final var cookies = request.getCookies();
        if (Objects.isNull(cookies)) {
            return null;
        }

        for (final var cookie : cookies) {
            if (nonNull(cookie) && cookieName.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

    private LinkedHashMap<String, Object> pageViewProperties(
            final HttpServletRequest request, final HttpServletResponse response) {
        final var properties = new LinkedHashMap<String, Object>();
        properties.put("$current_url", currentUrl(request));
        properties.put("$pathname", request.getRequestURI());
        properties.put("$referrer", request.getHeader("Referer"));
        properties.put("$user_agent", request.getHeader("User-Agent"));
        properties.put("method", request.getMethod());
        properties.put("status_code", response.getStatus());
        return properties;
    }

    private String currentUrl(final HttpServletRequest request) {
        final var queryString = request.getQueryString();
        final var baseUrl = request.getRequestURL().toString();
        if (Objects.isNull(queryString) || queryString.isBlank()) {
            return baseUrl;
        }

        return baseUrl + "?" + queryString;
    }
}
