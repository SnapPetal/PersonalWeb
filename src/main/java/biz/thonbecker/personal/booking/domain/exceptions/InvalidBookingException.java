package biz.thonbecker.personal.booking.domain.exceptions;

/**
 * Exception thrown when booking data is invalid.
 */
public class InvalidBookingException extends RuntimeException {

    public InvalidBookingException(final String message) {
        super(message);
    }
}
