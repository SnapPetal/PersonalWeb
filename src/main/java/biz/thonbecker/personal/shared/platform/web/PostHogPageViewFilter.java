package biz.thonbecker.personal.shared.platform.web;

import biz.thonbecker.personal.shared.platform.configuration.PostHogProperties;
import biz.thonbecker.personal.shared.platform.service.PostHogAnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Objects;
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

    private final PostHogAnalyticsService postHogAnalyticsService;
    private final PostHogProperties postHogProperties;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);

        if (!shouldCapture(request, response)) {
            return;
        }

        postHogAnalyticsService.capture(
                resolveDistinctId(request),
                "$pageview",
                pageViewProperties(request, response));
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

        if (Objects.nonNull(request.getHeader("HX-Request"))) {
            return false;
        }

        final var contentType = response.getContentType();
        return Objects.nonNull(contentType) && contentType.startsWith(MediaType.TEXT_HTML_VALUE);
    }

    private String resolveDistinctId(final HttpServletRequest request) {
        final Principal principal = request.getUserPrincipal();
        if (Objects.nonNull(principal)) {
            return principal.getName();
        }

        return request.getSession(true).getId();
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
