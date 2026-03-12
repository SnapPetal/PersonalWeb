package biz.thonbecker.personal.booking.platform.web.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Request to create an availability slot.
 */
public record CreateAvailabilitySlotRequest(
        @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {}
