package biz.thonbecker.personal.booking.api;

/**
 * Type of booking/meeting.
 *
 * @param id Database identifier
 * @param name Display name
 * @param description Description of what this meeting type is for
 * @param durationMinutes Length of the meeting in minutes
 * @param bufferMinutes Buffer time after the meeting in minutes
 * @param active Whether this booking type is currently available
 * @param color Hex color code for calendar display
 */
public record BookingType(
        Long id,
        String name,
        String description,
        int durationMinutes,
        int bufferMinutes,
        boolean active,
        String color) {}
