package biz.thonbecker.personal.landscape.platform;

import biz.thonbecker.personal.landscape.platform.service.LandscapeImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyLandscapeImageCleanup {

    private final JdbcTemplate jdbcTemplate;
    private final LandscapeImageStorageService imageStorageService;

    @EventListener(ApplicationReadyEvent.class)
    public void removeLegacyImages() {
        final var keys = jdbcTemplate.queryForList("SELECT s3_key FROM landscape.legacy_image_cleanup", String.class);
        keys.forEach(this::removeLegacyImage);
    }

    private void removeLegacyImage(final String s3Key) {
        try {
            imageStorageService.delete(s3Key);
            jdbcTemplate.update("DELETE FROM landscape.legacy_image_cleanup WHERE s3_key = ?", s3Key);
        } catch (final Exception exception) {
            log.warn("Legacy landscape image cleanup will retry for key {}", s3Key, exception);
        }
    }
}
