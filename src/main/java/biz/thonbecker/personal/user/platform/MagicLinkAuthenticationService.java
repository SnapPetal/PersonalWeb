package biz.thonbecker.personal.user.platform;

import biz.thonbecker.personal.user.api.UserAuthenticatedEvent;
import biz.thonbecker.personal.user.api.UserLoggedOutEvent;
import biz.thonbecker.personal.user.api.UserLoginEvent;
import biz.thonbecker.personal.user.api.UserLoginLinkRequestedEvent;
import biz.thonbecker.personal.user.api.UserSessionResolver;
import biz.thonbecker.personal.user.platform.persistence.UserLoginTokenEntity;
import biz.thonbecker.personal.user.platform.persistence.UserLoginTokenRepository;
import biz.thonbecker.personal.user.platform.persistence.UserService;
import biz.thonbecker.personal.user.platform.persistence.UserSessionEntity;
import biz.thonbecker.personal.user.platform.persistence.UserSessionRepository;
import java.net.URLEncoder;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MagicLinkAuthenticationService implements UserSessionResolver {

    private static final Duration LOGIN_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final int MAX_REQUESTS_PER_HOUR = 5;

    private final UserLoginTokenRepository loginTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void requestLoginLink(final String email, final String baseUrl, final String requestIp) {
        requestLoginLink(email, baseUrl, requestIp, "/landscape");
    }

    @Transactional
    public void requestLoginLink(
            final String email, final String baseUrl, final String requestIp, final String redirectPath) {
        final var normalizedEmail = normalizeEmail(email);
        final var since = Instant.now().minus(Duration.ofHours(1));
        if (loginTokenRepository.countRecentRequests(normalizedEmail, requestIp, since) >= MAX_REQUESTS_PER_HOUR) {
            return;
        }

        final var user = userService
                .findUserByEmail(normalizedEmail)
                .orElseGet(() -> userService.registerUser(normalizedEmail, normalizedEmail));
        final var token = UUID.randomUUID().toString() + UUID.randomUUID();
        final var now = Instant.now();
        final var loginToken = new UserLoginTokenEntity();
        loginToken.setTokenHash(hash(token));
        loginToken.setUserId(user.getId());
        loginToken.setEmail(normalizedEmail);
        loginToken.setRequestIp(requestIp);
        loginToken.setRequestedAt(now);
        loginToken.setExpiresAt(now.plus(LOGIN_TOKEN_TTL));
        loginTokenRepository.save(loginToken);

        eventPublisher.publishEvent(new UserLoginLinkRequestedEvent(
                normalizedEmail,
                baseUrl + "/auth/confirm?token=" + token + "&redirect="
                        + URLEncoder.encode(redirectPath, StandardCharsets.UTF_8),
                now));
    }

    @Transactional
    public Optional<Session> authenticate(final String token) {
        final var loginToken =
                loginTokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hash(token), Instant.now());
        if (loginToken.isEmpty()) {
            return Optional.empty();
        }

        final var tokenEntity = loginToken.get();
        final var now = Instant.now();
        tokenEntity.setUsedAt(now);
        loginTokenRepository.save(tokenEntity);
        final var sessionToken = UUID.randomUUID().toString() + UUID.randomUUID();
        final var session = new UserSessionEntity();
        session.setSessionHash(hash(sessionToken));
        session.setUserId(tokenEntity.getUserId());
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(SESSION_TTL));
        sessionRepository.save(session);
        eventPublisher.publishEvent(new UserAuthenticatedEvent(tokenEntity.getUserId(), tokenEntity.getEmail(), now));
        eventPublisher.publishEvent(new UserLoginEvent(tokenEntity.getUserId(), tokenEntity.getEmail(), now));
        return Optional.of(new Session(sessionToken, tokenEntity.getUserId(), now.plus(SESSION_TTL)));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> resolveUserId(final String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        return sessionRepository
                .findBySessionHashAndRevokedAtIsNullAndExpiresAtAfter(hash(sessionToken), Instant.now())
                .map(UserSessionEntity::getUserId);
    }

    @Transactional
    public void logout(final String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        sessionRepository
                .findBySessionHashAndRevokedAtIsNull(hash(sessionToken))
                .ifPresent(session -> {
                    session.setRevokedAt(Instant.now());
                    sessionRepository.save(session);
                    eventPublisher.publishEvent(new UserLoggedOutEvent(session.getUserId(), Instant.now()));
                });
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

    public record Session(String token, String userId, Instant expiresAt) {}
}
