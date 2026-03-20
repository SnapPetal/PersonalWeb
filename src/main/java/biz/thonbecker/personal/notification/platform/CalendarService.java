package biz.thonbecker.personal.notification.platform;

import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import org.springframework.stereotype.Service;

/**
 * Service for generating iCalendar (.ics) files for bookings.
 *
 * <p>Works with event data from shared events package.
 */
@Service
@Slf4j
class CalendarService {

    private static final String PRODUCT_ID = "-//Thon Becker//Booking System//EN";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Chicago");

    /**
     * Generates an iCalendar (.ics) file content for a booking with a random UID.
     *
     * @param event The booking created event
     * @param organizerEmail Email address of the organizer
     * @return iCalendar file content as a string
     */
    public String generateICalendar(final BookingCreatedEvent event, final String organizerEmail) {
        return generateICalendar(event, organizerEmail, null);
    }

    /**
     * Generates an iCalendar (.ics) file content for a booking.
     *
     * @param event The booking created event
     * @param organizerEmail Email address of the organizer
     * @param uid Optional deterministic UID for CalDAV compatibility (null for random)
     * @return iCalendar file content as a string
     */
    public String generateICalendar(final BookingCreatedEvent event, final String organizerEmail, final String uid) {
        try {
            // Create calendar
            final var calendar = new Calendar();
            calendar.add(new ProdId(PRODUCT_ID));
            calendar.add(new Version(new net.fortuna.ical4j.model.ParameterList(), "2.0"));
            calendar.add(new CalScale(CalScale.VALUE_GREGORIAN));

            // Convert LocalDateTime to ZonedDateTime
            final var startZoned = event.startTime().atZone(DEFAULT_ZONE);
            final var endZoned = event.endTime().atZone(DEFAULT_ZONE);

            // Create event
            final var vEvent = new VEvent(startZoned.toInstant(), endZoned.toInstant(), event.bookingTypeName());

            // Add UID (deterministic if provided, random otherwise)
            if (uid != null) {
                vEvent.add(new Uid(uid));
            } else {
                vEvent.add(new net.fortuna.ical4j.util.RandomUidGenerator().generateUid());
            }

            // Add description
            final var description = new StringBuilder();
            description.append("Booking Type: ").append(event.bookingTypeName()).append("\n");
            description.append("Attendee: ").append(event.attendeeName()).append("\n");
            description.append("Email: ").append(event.attendeeEmail()).append("\n");
            if (event.attendeePhone() != null && !event.attendeePhone().isBlank()) {
                description.append("Phone: ").append(event.attendeePhone()).append("\n");
            }
            if (event.message() != null && !event.message().isBlank()) {
                description.append("\nMessage:\n").append(event.message());
            }

            vEvent.add(new Description(description.toString()));

            // Add organizer
            final var organizer = new Organizer("mailto:" + organizerEmail);
            organizer.add(new Cn("Thon Becker"));
            vEvent.add(organizer);

            // Add attendee
            final var attendee = new Attendee("mailto:" + event.attendeeEmail());
            attendee.add(new Cn(event.attendeeName()));
            attendee.add(Role.REQ_PARTICIPANT);
            vEvent.add(attendee);

            // Add status
            vEvent.add(new Status(Status.VALUE_CONFIRMED));

            // Add to calendar
            calendar.add(vEvent);

            log.debug("Generated iCalendar for booking {}", event.confirmationCode());
            return calendar.toString();

        } catch (final Exception e) {
            log.error("Failed to generate iCalendar for booking {}: {}", event.confirmationCode(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate calendar file", e);
        }
    }
}
