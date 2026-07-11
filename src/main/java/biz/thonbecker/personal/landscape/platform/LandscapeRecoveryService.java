package biz.thonbecker.personal.landscape.platform;

import biz.thonbecker.personal.landscape.api.LandscapeRecoveryRequestedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LandscapeRecoveryService {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher events;

    @Transactional
    public void request(final String submittedEmail, final String currentOwnerId, final String baseUrl) {
        final var email = submittedEmail.trim().toLowerCase(Locale.ROOT);
        final var owners = jdbcTemplate.queryForList(
                "SELECT owner_id FROM landscape.recovery_emails WHERE email = ?", String.class, email);
        final var ownerId = owners.isEmpty() ? currentOwnerId : owners.getFirst();
        final var token = UUID.randomUUID().toString() + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO landscape.recovery_tokens (token_hash, email, owner_id, expires_at) VALUES (?, ?, ?, ?)",
                hash(token),
                email,
                ownerId,
                LocalDateTime.now().plusMinutes(15));
        events.publishEvent(
                new LandscapeRecoveryRequestedEvent(email, baseUrl + "/landscape/recovery/confirm?token=" + token));
    }

    @Transactional
    public Optional<String> confirm(final String token) {
        final var rows = jdbcTemplate.query(
                "SELECT email, owner_id FROM landscape.recovery_tokens WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP FOR UPDATE",
                (resultSet, row) -> new Recovery(resultSet.getString("email"), resultSet.getString("owner_id")),
                hash(token));
        if (rows.isEmpty()) return Optional.empty();
        final var recovery = rows.getFirst();
        jdbcTemplate.update(
                "UPDATE landscape.recovery_tokens SET used_at = CURRENT_TIMESTAMP WHERE token_hash = ?", hash(token));
        jdbcTemplate.update(
                "INSERT INTO landscape.recovery_emails (email, owner_id) VALUES (?, ?) ON CONFLICT (email) DO NOTHING",
                recovery.email(),
                recovery.ownerId());
        return Optional.of(recovery.ownerId());
    }

    private String hash(final String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Recovery(String email, String ownerId) {}
}
