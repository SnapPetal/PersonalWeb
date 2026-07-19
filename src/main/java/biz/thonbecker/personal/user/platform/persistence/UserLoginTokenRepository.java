package biz.thonbecker.personal.user.platform.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLoginTokenRepository extends JpaRepository<UserLoginTokenEntity, String> {

    @Query("""
            select count(token) from UserLoginTokenEntity token
            where (token.email = :email or token.requestIp = :requestIp)
              and token.requestedAt > :since
            """)
    long countRecentRequests(
            @Param("email") String email, @Param("requestIp") String requestIp, @Param("since") Instant since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserLoginTokenEntity> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash, Instant currentTime);

    long deleteByExpiresAtBeforeOrUsedAtBefore(Instant expiresAt, Instant usedAt);
}
