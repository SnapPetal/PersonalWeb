package biz.thonbecker.personal.user.platform.web;

import biz.thonbecker.personal.user.api.UserSessionResolver;
import biz.thonbecker.personal.user.platform.MagicLinkAuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthenticationController {

    private static final String SESSION_COOKIE = UserSessionResolver.SESSION_COOKIE_NAME;

    private final MagicLinkAuthenticationService authenticationService;

    @PostMapping("/request")
    ResponseEntity<Void> requestLoginLink(
            @RequestBody final LoginRequest loginRequest, final HttpServletRequest httpRequest) {
        authenticationService.requestLoginLink(
                loginRequest.email(),
                httpRequest.getRequestURL().toString().replace("/auth/request", ""),
                httpRequest.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/confirm")
    ResponseEntity<Void> confirm(
            @RequestParam final String token, final HttpServletRequest request, final HttpServletResponse response) {
        return authenticationService
                .authenticate(token)
                .map(session -> {
                    final var cookie = new Cookie(SESSION_COOKIE, session.token());
                    cookie.setHttpOnly(true);
                    cookie.setSecure(
                            request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
                    cookie.setPath("/");
                    cookie.setMaxAge((int) Duration.ofHours(24).toSeconds());
                    response.addCookie(cookie);
                    return ResponseEntity.status(302)
                            .header("Location", "/landscape")
                            .<Void>build();
                })
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = SESSION_COOKIE, required = false) final String sessionToken,
            final HttpServletResponse response) {
        authenticationService.logout(sessionToken);
        final var cookie = new Cookie(SESSION_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    ResponseEntity<String> me(@CookieValue(name = SESSION_COOKIE, required = false) final String sessionToken) {
        return authenticationService
                .resolveUserId(sessionToken)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    record LoginRequest(String email) {}
}
