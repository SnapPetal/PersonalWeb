package biz.thonbecker.personal.booking.api;

import java.time.LocalDateTime;

/**
 * Event published when a new booking is created.
 *
 * <p>Contains all data needed for notification sending without requiring
 * additional lookups from the booking module.
 *
 * @param bookingId Booking identifier
 * @param confirmationCode Booking confirmation code
 * @param attendeeEmail Attendee's email address
 * @param attendeeName Attendee's name
 * @param attendeePhone Attendee's phone number (optional)
 * @param bookingTypeName Type of booking
 * @param startTime Scheduled start time
 * @param endTime Scheduled end time
 * @param message Optional message from attendee
 */
public record BookingCreatedEvent(
        Long bookingId,
        String confirmationCode,
        String attendeeEmail,
        String attendeeName,
        String attendeePhone,
        String bookingTypeName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String message) {}
