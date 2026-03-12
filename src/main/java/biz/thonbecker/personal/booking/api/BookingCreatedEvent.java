package biz.thonbecker.personal.booking.api;

import java.time.LocalDateTime;

/**
 * Event published when a new booking is created.
 *
 * @param bookingId Booking identifier
 * @param confirmationCode Booking confirmation code
 * @param attendeeEmail Attendee's email address
 * @param attendeeName Attendee's name
 * @param bookingTypeName Type of booking
 * @param startTime Scheduled start time
 */
public record BookingCreatedEvent(
        Long bookingId,
        String confirmationCode,
        String attendeeEmail,
        String attendeeName,
        String bookingTypeName,
        LocalDateTime startTime) {}
