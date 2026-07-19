package biz.thonbecker.personal.user.platform;

import biz.thonbecker.personal.user.platform.persistence.UserLoginTokenRepository;
import biz.thonbecker.personal.user.platform.persistence.UserSessionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes expired passwordless authentication artifacts. */
@Service
@RequiredArgsConstructor
public class AuthenticationCleanupService {

    private final UserLoginTokenRepository loginTokenRepository;
    private final UserSessionRepository sessionRepository;

    @Scheduled(cron = "0 15 * * * *")
    @SchedulerLock(name = "cleanupUserAuthentication", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    @Transactional
    public void cleanupExpiredAuthenticationData() {
        final var now = Instant.now();
        loginTokenRepository.deleteByExpiresAtBeforeOrUsedAtBefore(now, now.minusSeconds(86_400));
        sessionRepository.deleteByExpiresAtBeforeOrRevokedAtBefore(now, now.minusSeconds(604_800));
    }
}
