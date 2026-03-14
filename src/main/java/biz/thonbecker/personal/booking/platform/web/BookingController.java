package biz.thonbecker.personal.booking.platform.web;

import biz.thonbecker.personal.booking.api.Booking;
import biz.thonbecker.personal.booking.platform.BookingService;
import biz.thonbecker.personal.booking.platform.web.model.CreateBookingRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Public web controller for booking functionality.
 */
@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * Main booking page.
     *
     * @param model Spring MVC model
     * @return Thymeleaf template name
     */
    @GetMapping
    public String bookingPage(final Model model) {
        final var bookingTypes = bookingService.getActiveBookingTypes();
        model.addAttribute("bookingTypes", bookingTypes);
        return "booking/index";
    }

    /**
     * Get available slots for a booking type and date (HTMX endpoint).
     *
     * @param bookingTypeId Booking type identifier
     * @param date Date to check availability
     * @param model Spring MVC model
     * @return Thymeleaf fragment with time slots
     */
    @GetMapping("/types/{bookingTypeId}/slots")
    public String getAvailableSlots(
            @PathVariable final Long bookingTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            final Model model) {

        try {
            log.debug("Fetching available slots for type {} on {}", bookingTypeId, date);
            final var slots = bookingService.getAvailableSlots(bookingTypeId, date);
            model.addAttribute("slots", slots);
            model.addAttribute("bookingTypeId", bookingTypeId);
            model.addAttribute("date", date);
            log.info("Found {} available slots", slots.size());
        } catch (final Exception e) {
            log.error("Failed to fetch available slots: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load available time slots. Please try again.");
        }

        return "booking/fragments :: time-slots";
    }

    /**
     * Creates a new booking.
     *
     * @param request Booking details
     * @param principal Authenticated user (optional)
     * @return Created booking
     */
    @PostMapping("/book")
    @ResponseBody
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody final CreateBookingRequest request, final Principal principal) {

        try {
            final var userId = principal != null ? principal.getName() : null;

            log.info(
                    "Creating booking for type {} at {} (user: {})",
                    request.bookingTypeId(),
                    request.startTime(),
                    userId);

            final var booking = bookingService.createBooking(
                    request.bookingTypeId(),
                    request.attendeeName(),
                    request.attendeeEmail(),
                    request.attendeePhone(),
                    request.startTime(),
                    request.message(),
                    userId);

            log.info("Successfully created booking with confirmation code: {}", booking.confirmationCode());
            return ResponseEntity.ok(booking);

        } catch (final Exception e) {
            log.error("Failed to create booking: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * View booking confirmation page.
     *
     * @param confirmationCode Booking confirmation code
     * @param model Spring MVC model
     * @return Thymeleaf template name
     */
    @GetMapping("/confirmation/{confirmationCode}")
    public String viewBooking(@PathVariable final String confirmationCode, final Model model) {
        try {
            final var booking = bookingService.getBookingByConfirmationCode(confirmationCode);
            model.addAttribute("booking", booking);
            return "booking/confirmation";
        } catch (final Exception e) {
            log.error("Booking not found: {}", confirmationCode, e);
            model.addAttribute("error", "Booking not found");
            return "booking/not-found";
        }
    }

    /**
     * Cancel a booking.
     *
     * @param confirmationCode Booking confirmation code
     * @return Success response
     */
    @PostMapping("/confirmation/{confirmationCode}/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelBooking(@PathVariable final String confirmationCode) {
        try {
            log.info("Cancelling booking: {}", confirmationCode);
            final var booking = bookingService.getBookingByConfirmationCode(confirmationCode);
            bookingService.cancelBooking(booking.id());
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to cancel booking: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
