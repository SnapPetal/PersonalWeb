package biz.thonbecker.personal.booking.platform.service;

import biz.thonbecker.personal.booking.api.Booking;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import org.springframework.stereotype.Service;

/**
 * Service for generating iCalendar (.ics) files for bookings.
 */
@Service
@Slf4j
public class CalendarService {

    private static final String PRODUCT_ID = "-//Thon Becker//Booking System//EN";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/New_York");

    /**
     * Generates an iCalendar (.ics) file content for a booking.
     *
     * @param booking The booking to create a calendar event for
     * @param organizerEmail Email address of the organizer
     * @return iCalendar file content as a string
     */
    public String generateICalendar(final Booking booking, final String organizerEmail) {
        try {
            // Create calendar
            final var calendar = new Calendar();
            calendar.add(new ProdId(PRODUCT_ID));
            calendar.add(new Version());
            calendar.add(new CalScale(CalScale.VALUE_GREGORIAN));

            // Convert LocalDateTime to ZonedDateTime
            final var startZoned = booking.startTime().atZone(DEFAULT_ZONE);
            final var endZoned = booking.endTime().atZone(DEFAULT_ZONE);

            // Create event
            final var event = new VEvent(
                    startZoned.toInstant(),
                    endZoned.toInstant(),
                    booking.bookingType().name());

            // Add unique ID
            final var uidGenerator = new RandomUidGenerator();
            event.add(uidGenerator.generateUid());

            // Add description
            final var description = new StringBuilder();
            description
                    .append("Booking Type: ")
                    .append(booking.bookingType().name())
                    .append("\n");
            description.append("Attendee: ").append(booking.attendeeName()).append("\n");
            description.append("Email: ").append(booking.attendeeEmail()).append("\n");
            if (booking.attendeePhone() != null && !booking.attendeePhone().isBlank()) {
                description.append("Phone: ").append(booking.attendeePhone()).append("\n");
            }
            if (booking.message() != null && !booking.message().isBlank()) {
                description.append("\nMessage:\n").append(booking.message());
            }

            event.add(new Description(description.toString()));

            // Add organizer
            final var organizer = new Organizer("mailto:" + organizerEmail);
            organizer.add(new Cn("Thon Becker"));
            event.add(organizer);

            // Add attendee
            final var attendee = new Attendee("mailto:" + booking.attendeeEmail());
            attendee.add(new Cn(booking.attendeeName()));
            attendee.add(Role.REQ_PARTICIPANT);
            event.add(attendee);

            // Add status
            event.add(new Status(Status.VALUE_CONFIRMED));

            // Add to calendar
            calendar.add(event);

            log.debug("Generated iCalendar for booking {}", booking.confirmationCode());
            return calendar.toString();

        } catch (final Exception e) {
            log.error("Failed to generate iCalendar for booking {}: {}", booking.confirmationCode(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate calendar file", e);
        }
    }
}
