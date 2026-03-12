package biz.thonbecker.personal.booking.domain.exceptions;

/**
 * Exception thrown when a booking cannot be found.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(final Long bookingId) {
        super("Booking not found: " + bookingId);
    }

    public BookingNotFoundException(final String confirmationCode) {
        super("Booking not found with confirmation code: " + confirmationCode);
    }
}
