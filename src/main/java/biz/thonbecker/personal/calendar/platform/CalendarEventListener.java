package biz.thonbecker.personal.calendar.platform;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.calendar.platform.persistence.CalendarEventMappingEntity;
import biz.thonbecker.personal.calendar.platform.persistence.CalendarEventMappingRepository;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for booking events and manages corresponding Nextcloud calendar entries.
 *
 * <p>Uses {@link ApplicationModuleListener} so Spring Modulith persists the event publication,
 * invokes the listener after commit, and retries it if the integration work fails.
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

    @ApplicationModuleListener
    void onBookingCreated(final BookingCreatedEvent event) {
        if (!properties.enabled() || Objects.isNull(calDavService)) {
            log.debug("Nextcloud integration disabled, skipping calendar event creation");
            return;
        }

        final var calendarUid = calDavService.createEvent(event);

        final var mapping =
                mappingRepository.findByBookingId(event.bookingId()).orElseGet(CalendarEventMappingEntity::new);
        mapping.setBookingId(event.bookingId());
        mapping.setCalendarUid(calendarUid);
        mapping.setCalendarHref(calendarUid + ".ics");
        mappingRepository.save(mapping);

        log.info("Created Nextcloud calendar event {} for booking {}", calendarUid, event.confirmationCode());
    }

    @ApplicationModuleListener
    void onBookingCancelled(final BookingCancelledEvent event) {
        if (!properties.enabled() || Objects.isNull(calDavService)) {
            return;
        }

        final var mapping = mappingRepository.findByBookingId(event.bookingId());
        final var calendarUid =
                mapping.map(CalendarEventMappingEntity::getCalendarUid).orElse("booking-" + event.confirmationCode());

        calDavService.deleteEvent(calendarUid);
        mapping.ifPresent(mappingRepository::delete);
    }
}
