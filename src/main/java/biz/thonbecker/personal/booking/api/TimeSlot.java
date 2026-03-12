package biz.thonbecker.personal.booking.api;

import java.time.LocalDateTime;

/**
 * An available time slot for booking.
 *
 * @param id Database identifier
 * @param startTime When the slot starts
 * @param endTime When the slot ends
 * @param available Whether the slot is still available for booking
 */
public record TimeSlot(Long id, LocalDateTime startTime, LocalDateTime endTime, boolean available) {}
