package biz.thonbecker.personal.calendar.platform;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.calendar.platform.persistence.CalendarEventMappingEntity;
import biz.thonbecker.personal.calendar.platform.persistence.CalendarEventMappingRepository;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for booking events and manages corresponding Nextcloud calendar entries.
 *
 * <p>Uses {@link Async} so CalDAV calls to Nextcloud don't block the booking HTTP response.
 */
@Component
@Slf4j
class CalendarEventListener {

    private final NextcloudCalDavService calDavService;
    private final CalendarEventMappingRepository mappingRepository;
    private final CalendarProperties properties;

    CalendarEventListener(
            final ObjectProvider<NextcloudCalDavService> calDavService,
            final CalendarEventMappingRepository mappingRepository,
            final CalendarProperties properties) {
        this.calDavService = calDavService.getIfAvailable();
        this.mappingRepository = mappingRepository;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void onBookingCreated(final BookingCreatedEvent event) {
        if (!properties.enabled() || Objects.isNull(calDavService)) {
            log.debug("Nextcloud integration disabled, skipping calendar event creation");
            return;
        }

        try {
            final var calendarUid = calDavService.createEvent(event);

            final var mapping = new CalendarEventMappingEntity();
            mapping.setBookingId(event.bookingId());
            mapping.setCalendarUid(calendarUid);
            mapping.setCalendarHref(calendarUid + ".ics");
            mappingRepository.save(mapping);

            log.info("Created Nextcloud calendar event {} for booking {}", calendarUid, event.confirmationCode());
        } catch (final Exception e) {
            log.error(
                    "Failed to create Nextcloud calendar event for booking {}: {}",
                    event.confirmationCode(),
                    e.getMessage(),
                    e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void onBookingCancelled(final BookingCancelledEvent event) {
        if (!properties.enabled() || Objects.isNull(calDavService)) {
            return;
        }

        final var mapping = mappingRepository.findByBookingId(event.bookingId());
        if (mapping.isEmpty()) {
            log.debug("No calendar mapping found for booking {}, skipping CalDAV deletion", event.bookingId());
            return;
        }

        try {
            calDavService.deleteEvent(mapping.get().getCalendarUid());
        } catch (final Exception e) {
            log.warn("Failed to delete Nextcloud calendar event for booking {}: {}", event.bookingId(), e.getMessage());
        }
        mappingRepository.delete(mapping.get());
    }
}
