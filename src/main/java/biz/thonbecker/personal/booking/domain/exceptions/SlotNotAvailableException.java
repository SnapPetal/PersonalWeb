package biz.thonbecker.personal.booking.domain.exceptions;

import java.time.LocalDateTime;

/**
 * Exception thrown when attempting to book a time slot that is not available.
 */
public class SlotNotAvailableException extends RuntimeException {

    public SlotNotAvailableException(final LocalDateTime startTime) {
        super("Time slot not available: " + startTime);
    }

    public SlotNotAvailableException(final String message) {
        super(message);
    }
}
