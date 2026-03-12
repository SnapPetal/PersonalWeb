package biz.thonbecker.personal.booking.platform.web.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Request to create a new booking.
 */
public record CreateBookingRequest(
        @NotNull Long bookingTypeId,
        @NotBlank String attendeeName,
        @NotBlank @Email String attendeeEmail,
        String attendeePhone,
        @NotNull LocalDateTime startTime,
        String message) {}
