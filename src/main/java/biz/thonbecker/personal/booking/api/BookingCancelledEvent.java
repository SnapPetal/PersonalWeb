package biz.thonbecker.personal.booking.api;

/**
 * Event published when a booking is cancelled.
 *
 * @param bookingId Booking identifier
 * @param confirmationCode Booking confirmation code
 * @param attendeeEmail Attendee's email address
 */
public record BookingCancelledEvent(Long bookingId, String confirmationCode, String attendeeEmail) {}
