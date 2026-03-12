package biz.thonbecker.personal.booking.api;

/**
 * Status of a booking.
 */
public enum BookingStatus {
    /** Booking has been created and is pending confirmation. */
    PENDING,

    /** Booking has been confirmed. */
    CONFIRMED,

    /** Booking has been cancelled. */
    CANCELLED,

    /** Booking has been completed (past the scheduled time). */
    COMPLETED
}
