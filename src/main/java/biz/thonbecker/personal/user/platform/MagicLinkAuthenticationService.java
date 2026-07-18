package biz.thonbecker.personal.user.platform;

import biz.thonbecker.personal.user.api.UserAuthenticatedEvent;
import biz.thonbecker.personal.user.api.UserLoggedOutEvent;
import biz.thonbecker.personal.user.api.UserLoginEvent;
import biz.thonbecker.personal.user.api.UserLoginLinkRequestedEvent;
import biz.thonbecker.personal.user.api.UserSessionResolver;
import biz.thonbecker.personal.user.platform.persistence.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MagicLinkAuthenticationService implements UserSessionResolver {

    private static final Duration LOGIN_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final int MAX_REQUESTS_PER_HOUR = 5;

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void requestLoginLink(final String email, final String baseUrl, final String requestIp) {
        final var normalizedEmail = normalizeEmail(email);
        final var since = Instant.now().minus(Duration.ofHours(1));
        final var requestCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trivia.user_login_tokens WHERE (email = ? OR request_ip = ?) AND requested_at > ?",
                Integer.class,
                normalizedEmail,
                requestIp,
                since);
        if (requestCount != null && requestCount >= MAX_REQUESTS_PER_HOUR) {
            return;
        }

        final var user = userService
                .findUserByEmail(normalizedEmail)
                .orElseGet(() -> userService.registerUser(normalizedEmail, normalizedEmail));
        final var token = UUID.randomUUID().toString() + UUID.randomUUID();
        final var now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO trivia.user_login_tokens (token_hash, user_id, email, request_ip, requested_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
                hash(token),
                user.getId(),
                normalizedEmail,
                requestIp,
                now,
                now.plus(LOGIN_TOKEN_TTL));

        eventPublisher.publishEvent(
                new UserLoginLinkRequestedEvent(normalizedEmail, baseUrl + "/auth/confirm?token=" + token, now));
    }

    @Transactional
    public Optional<Session> authenticate(final String token) {
        final var rows = jdbcTemplate.query(
                "SELECT user_id, email FROM trivia.user_login_tokens WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP FOR UPDATE",
                (resultSet, rowNum) -> new LoginToken(resultSet.getString("user_id"), resultSet.getString("email")),
                hash(token));
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        final var loginToken = rows.getFirst();
        final var now = Instant.now();
        jdbcTemplate.update("UPDATE trivia.user_login_tokens SET used_at = ? WHERE token_hash = ?", now, hash(token));
        final var sessionToken = UUID.randomUUID().toString() + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO trivia.user_sessions (session_hash, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
                hash(sessionToken),
                loginToken.userId(),
                now,
                now.plus(SESSION_TTL));
        eventPublisher.publishEvent(new UserAuthenticatedEvent(loginToken.userId(), loginToken.email(), now));
        eventPublisher.publishEvent(new UserLoginEvent(loginToken.userId(), loginToken.email(), now));
        return Optional.of(new Session(sessionToken, loginToken.userId(), now.plus(SESSION_TTL)));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> resolveUserId(final String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        final var rows = jdbcTemplate.query(
                "SELECT user_id FROM trivia.user_sessions WHERE session_hash = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP",
                (resultSet, rowNum) -> resultSet.getString("user_id"),
                hash(sessionToken));
        return rows.stream().findFirst();
    }

    @Transactional
    public void logout(final String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        final var userIds = jdbcTemplate.query(
                "SELECT user_id FROM trivia.user_sessions WHERE session_hash = ? AND revoked_at IS NULL",
                (resultSet, rowNum) -> resultSet.getString("user_id"),
                hash(sessionToken));
        jdbcTemplate.update(
                "UPDATE trivia.user_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE session_hash = ? AND revoked_at IS NULL",
                hash(sessionToken));
        userIds.stream()
                .findFirst()
                .ifPresent(userId -> eventPublisher.publishEvent(new UserLoggedOutEvent(userId, Instant.now())));
    }

    private String normalizeEmail(final String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        return email.trim().toLowerCase();
    }

    private String hash(final String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record LoginToken(String userId, String email) {}

    public record Session(String token, String userId, Instant expiresAt) {}
}
