package biz.thonbecker.personal.shared.events;

import java.time.LocalDateTime;

/**
 * Event published when a booking is cancelled.
 *
 * <p>Contains all data needed for notification sending without requiring
 * additional lookups from the booking module.
 *
 * @param bookingId Booking identifier
 * @param confirmationCode Booking confirmation code
 * @param attendeeEmail Attendee's email address
 * @param attendeeName Attendee's name
 * @param bookingTypeName Type of booking
 * @param startTime Scheduled start time
 * @param endTime Scheduled end time
 */
public record BookingCancelledEvent(
        Long bookingId,
        String confirmationCode,
        String attendeeEmail,
        String attendeeName,
        String bookingTypeName,
        LocalDateTime startTime,
        LocalDateTime endTime) {}
