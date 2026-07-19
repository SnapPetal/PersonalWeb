package biz.thonbecker.personal.user.platform.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    Optional<UserSessionEntity> findBySessionHashAndRevokedAtIsNullAndExpiresAtAfter(
            String sessionHash, Instant currentTime);

    Optional<UserSessionEntity> findBySessionHashAndRevokedAtIsNull(String sessionHash);

    long deleteByExpiresAtBeforeOrRevokedAtBefore(Instant expiresAt, Instant revokedAt);
}
