package biz.thonbecker.personal.user.platform;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes expired passwordless authentication artifacts. */
@Service
@RequiredArgsConstructor
public class AuthenticationCleanupService {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 15 * * * *")
    @SchedulerLock(name = "cleanupUserAuthentication", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    @Transactional
    public void cleanupExpiredAuthenticationData() {
        jdbcTemplate.update(
                "DELETE FROM identity.user_login_tokens WHERE expires_at < CURRENT_TIMESTAMP OR used_at < CURRENT_TIMESTAMP - INTERVAL '1 day'");
        jdbcTemplate.update(
                "DELETE FROM identity.user_sessions WHERE expires_at < CURRENT_TIMESTAMP OR revoked_at < CURRENT_TIMESTAMP - INTERVAL '7 days'");
    }
}
