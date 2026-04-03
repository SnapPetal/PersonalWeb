package biz.thonbecker.personal.calendar.platform;

import biz.thonbecker.personal.booking.api.BookingCancellationRequestedEvent;
import biz.thonbecker.personal.calendar.platform.persistence.CalendarEventMappingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the Nextcloud Bookings calendar for deleted events.
 *
 * <p>When a calendar event is deleted from Nextcloud (e.g., via the web UI or a calendar app),
 * this scheduler detects it and publishes a {@link BookingCancellationRequestedEvent} so the
 * booking module can cancel the corresponding booking.
 */
@Service
@ConditionalOnProperty(name = "calendar.nextcloud.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class CalendarSyncScheduler {

    private final NextcloudCalDavService calDavService;
    private final CalendarEventMappingRepository mappingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @PostConstruct
    void init() {
        try {
            calDavService.ensureCalendarExists();
        } catch (final Exception e) {
            log.warn("Failed to ensure Nextcloud calendar exists on startup: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${calendar.nextcloud.sync-cron:0 */5 * * * *}")
    @SchedulerLock(name = "syncNextcloudCalendar", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
    @Transactional
    public void syncCalendarDeletions() {
        log.debug("Checking Nextcloud calendar for deleted events...");

        try {
            final var existingUids = calDavService.listEventResources();
            final var allMappings = mappingRepository.findAll();

            var deletedCount = 0;
            for (final var mapping : allMappings) {
                if (!existingUids.contains(mapping.getCalendarUid())) {
                    log.info(
                            "Calendar event {} deleted from Nextcloud, requesting cancellation for booking {}",
                            mapping.getCalendarUid(),
                            mapping.getBookingId());

                    eventPublisher.publishEvent(new BookingCancellationRequestedEvent(mapping.getBookingId()));
                    deletedCount++;
                }
            }

            if (deletedCount > 0) {
                log.info("Detected {} deleted calendar events, published cancellation requests", deletedCount);
            } else {
                log.debug("No deleted calendar events detected");
            }
        } catch (final Exception e) {
            log.error("Failed to sync Nextcloud calendar: {}", e.getMessage(), e);
        }
    }
}
