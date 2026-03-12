package biz.thonbecker.personal.booking.domain.exceptions;

/**
 * Exception thrown when a booking type cannot be found.
 */
public class BookingTypeNotFoundException extends RuntimeException {

    public BookingTypeNotFoundException(final Long bookingTypeId) {
        super("Booking type not found: " + bookingTypeId);
    }
}
