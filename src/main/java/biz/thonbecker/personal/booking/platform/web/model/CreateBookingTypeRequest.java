package biz.thonbecker.personal.booking.platform.web.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to create a booking type.
 */
public record CreateBookingTypeRequest(
        @NotBlank String name,
        @NotBlank String description,
        @Min(5) int durationMinutes,
        @Min(0) int bufferMinutes,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {}
