package biz.thonbecker.personal.foosball.platform;

import biz.thonbecker.personal.foosball.platform.persistence.GameRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {
    private final GameRepository gameRepository;

    @Scheduled(cron = "0 0 0 1 * ?") // Run at midnight on the first day of every month
    @SchedulerLock(name = "cleanupOldGames", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void cleanupOldGames() {
        log.info("Starting cleanup of old games.");
        final var ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);
        final var deletedGamesCount = gameRepository.deleteGamesOlderThan(ninetyDaysAgo);
        log.info("Finished cleanup of old games. Deleted {} games.", deletedGamesCount);
    }
}
