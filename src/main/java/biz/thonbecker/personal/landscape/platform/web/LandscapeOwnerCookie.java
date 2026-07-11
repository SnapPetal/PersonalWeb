package biz.thonbecker.personal.landscape.platform.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class LandscapeOwnerCookie {

    static final String COOKIE_NAME = "LANDSCAPE_OWNER";
    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("anon:[0-9a-f-]{36}");
    private static final Duration MAX_AGE = Duration.ofDays(365 * 2L);

    public String resolve(final HttpServletRequest request, final HttpServletResponse response) {
        final var existingOwnerId = findOwnerId(request);
        if (Objects.nonNull(existingOwnerId)) {
            return existingOwnerId;
        }

        final var ownerId = "anon:" + UUID.randomUUID();
        write(ownerId, request, response);
        return ownerId;
    }

    public void restore(final String ownerId, final HttpServletRequest request, final HttpServletResponse response) {
        write(ownerId, request, response);
    }

    private void write(final String ownerId, final HttpServletRequest request, final HttpServletResponse response) {
        final var cookie = ResponseCookie.from(COOKIE_NAME, ownerId)
                .httpOnly(true)
                .secure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")))
                .sameSite("Lax")
                .path("/landscape")
                .maxAge(MAX_AGE)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String findOwnerId(final HttpServletRequest request) {
        if (Objects.isNull(request.getCookies())) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> OWNER_ID_PATTERN.matcher(value).matches())
                .findFirst()
                .orElse(null);
    }
}
