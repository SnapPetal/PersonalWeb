package biz.thonbecker.personal.booking.api;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A booking/appointment.
 *
 * @param id Database identifier
 * @param confirmationCode Unique code for looking up the booking
 * @param bookingType Type of meeting
 * @param attendeeName Name of the person booking
 * @param attendeeEmail Email address
 * @param attendeePhone Phone number (optional)
 * @param startTime Scheduled start time
 * @param endTime Scheduled end time
 * @param message Optional message from the attendee
 * @param status Current status
 * @param userId User ID if booked by authenticated user (optional)
 * @param createdAt When the booking was created
 */
public record Booking(
        Long id,
        String confirmationCode,
        BookingType bookingType,
        String attendeeName,
        String attendeeEmail,
        String attendeePhone,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String message,
        BookingStatus status,
        String userId,
        Instant createdAt) {}
