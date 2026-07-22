package biz.thonbecker.personal.user.platform.web;

import biz.thonbecker.personal.user.api.UserSessionResolver;
import biz.thonbecker.personal.user.platform.MagicLinkAuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthenticationController {

    private static final String SESSION_COOKIE = UserSessionResolver.SESSION_COOKIE_NAME;
    private static final String PUBLIC_HOME = "https://thonbecker.biz";

    private final MagicLinkAuthenticationService authenticationService;

    @GetMapping("/login")
    String login(@RequestParam(required = false) final String redirect, final Model model) {
        if (isUnknownRedirect(redirect)) {
            return "redirect:" + PUBLIC_HOME;
        }
        model.addAttribute("redirect", safeRedirect(redirect));
        model.addAttribute("loginDestinations", LoginDestinationRegistry.destinations());
        return "auth/login";
    }

    @PostMapping("/request")
    String requestLoginLink(
            @RequestParam("email") final String email,
            @RequestParam(required = false) final String redirect,
            final HttpServletRequest httpRequest) {
        final var safeRedirectPath = safeRedirect(redirect);
        if (safeRedirectPath == null) {
            return isUnknownRedirect(redirect) ? "redirect:" + PUBLIC_HOME : "redirect:/auth/login";
        }
        try {
            authenticationService.requestLoginLink(
                    email,
                    httpRequest.getRequestURL().toString().replace("/auth/request", ""),
                    httpRequest.getRemoteAddr(),
                    safeRedirectPath);
            return "redirect:/auth/login?sent&redirect=" + safeRedirectPath;
        } catch (final IllegalArgumentException exception) {
            return "redirect:/auth/login?error&redirect=" + safeRedirectPath;
        }
    }

    @GetMapping("/confirm")
    ResponseEntity<Void> confirm(
            @RequestParam final String token,
            @RequestParam(required = false) final String redirect,
            final HttpServletRequest request,
            final HttpServletResponse response) {
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
                    final var safeRedirectPath = safeRedirect(redirect);
                    return ResponseEntity.status(302)
                            .header(
                                    "Location",
                                    safeRedirectPath == null
                                            ? (isUnknownRedirect(redirect) ? PUBLIC_HOME : "/auth/login")
                                            : safeRedirectPath)
                            .<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(302)
                        .header("Location", "/auth/login?invalid")
                        .build());
    }

    private String safeRedirect(final String redirect) {
        return LoginDestinationRegistry.findPath(redirect).orElse(null);
    }

    private boolean isUnknownRedirect(final String redirect) {
        return redirect != null && !redirect.isBlank() && safeRedirect(redirect) == null;
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = SESSION_COOKIE, required = false) final String sessionToken,
            @RequestParam(defaultValue = "/landscape") final String redirect,
            final HttpServletResponse response) {
        authenticationService.logout(sessionToken);
        final var cookie = new Cookie(SESSION_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        final var safeRedirectPath = safeRedirect(redirect);
        final var loginLocation = safeRedirectPath == null
                ? (isUnknownRedirect(redirect) ? PUBLIC_HOME : "/auth/login")
                : "/auth/login?redirect=" + URLEncoder.encode(safeRedirectPath, StandardCharsets.UTF_8);
        return ResponseEntity.status(302).header("Location", loginLocation).build();
    }

    @GetMapping("/me")
    ResponseEntity<String> me(@CookieValue(name = SESSION_COOKIE, required = false) final String sessionToken) {
        return authenticationService
                .resolveUserId(sessionToken)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
