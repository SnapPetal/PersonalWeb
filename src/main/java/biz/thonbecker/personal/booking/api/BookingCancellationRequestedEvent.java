package biz.thonbecker.personal.booking.api;

/**
 * Event requesting cancellation of a booking.
 *
 * <p>Published by external modules (e.g., calendar) when they detect a booking
 * should be cancelled. The booking module listens for this and performs the cancellation.
 *
 * @param bookingId The booking ID to cancel
 */
public record BookingCancellationRequestedEvent(Long bookingId) {}
