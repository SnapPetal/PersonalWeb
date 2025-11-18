package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.infrastructure.persistence.GameRepository;
import java.time.LocalDate;
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
        final var ninetyDaysAgo = LocalDate.now().minusDays(90);
        final var deletedGamesCount = gameRepository.deleteGamesOlderThan(ninetyDaysAgo.atStartOfDay());
        log.info("Finished cleanup of old games. Deleted {} games.", deletedGamesCount);
    }
}
