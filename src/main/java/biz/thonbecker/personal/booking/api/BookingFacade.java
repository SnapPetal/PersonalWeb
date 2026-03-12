package biz.thonbecker.personal.booking.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public facade for the Booking module.
 *
 * <p>This is the ONLY entry point for other modules to interact with booking functionality.
 */
public interface BookingFacade {

    /**
     * Retrieves all active booking types.
     *
     * @return List of active booking types
     */
    List<BookingType> getActiveBookingTypes();

    /**
     * Retrieves all booking types (including inactive).
     *
     * @return List of all booking types
     */
    List<BookingType> getAllBookingTypes();

    /**
     * Retrieves available time slots for a specific booking type and date.
     *
     * @param bookingTypeId Booking type identifier
     * @param date Date to check availability
     * @return List of available time slots
     */
    List<TimeSlot> getAvailableSlots(Long bookingTypeId, LocalDate date);

    /**
     * Creates a new booking.
     *
     * @param bookingTypeId Type of booking
     * @param attendeeName Attendee's name
     * @param attendeeEmail Attendee's email
     * @param attendeePhone Attendee's phone (optional)
     * @param startTime Desired start time
     * @param message Optional message
     * @param userId User ID if authenticated (optional)
     * @return The created booking
     */
    Booking createBooking(
            Long bookingTypeId,
            String attendeeName,
            String attendeeEmail,
            String attendeePhone,
            LocalDateTime startTime,
            String message,
            String userId);

    /**
     * Retrieves a booking by confirmation code.
     *
     * @param confirmationCode Confirmation code
     * @return The booking
     */
    Booking getBookingByConfirmationCode(String confirmationCode);

    /**
     * Retrieves a booking by ID.
     *
     * @param bookingId Booking identifier
     * @return The booking
     */
    Booking getBooking(Long bookingId);

    /**
     * Retrieves all bookings for a user.
     *
     * @param userId User identifier
     * @return List of user's bookings
     */
    List<Booking> getUserBookings(String userId);

    /**
     * Retrieves all bookings (admin function).
     *
     * @return List of all bookings
     */
    List<Booking> getAllBookings();

    /**
     * Cancels a booking.
     *
     * @param bookingId Booking identifier
     */
    void cancelBooking(Long bookingId);

    /**
     * Creates a new booking type (admin function).
     *
     * @param name Display name
     * @param description Description
     * @param durationMinutes Meeting duration
     * @param bufferMinutes Buffer time after meeting
     * @param color Hex color code
     * @return The created booking type
     */
    BookingType createBookingType(
            String name, String description, int durationMinutes, int bufferMinutes, String color);

    /**
     * Creates an availability slot (admin function).
     *
     * @param startTime Slot start time
     * @param endTime Slot end time
     */
    void createAvailabilitySlot(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Deletes an availability slot (admin function).
     *
     * @param slotId Slot identifier
     */
    void deleteAvailabilitySlot(Long slotId);

    /**
     * Retrieves all availability slots.
     *
     * @return List of all availability slots
     */
    List<TimeSlot> getAllAvailabilitySlots();
}
