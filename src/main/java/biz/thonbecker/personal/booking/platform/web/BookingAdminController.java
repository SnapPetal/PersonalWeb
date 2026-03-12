package biz.thonbecker.personal.booking.platform.web;

import biz.thonbecker.personal.booking.api.BookingFacade;
import biz.thonbecker.personal.booking.api.BookingType;
import biz.thonbecker.personal.booking.platform.web.model.CreateAvailabilitySlotRequest;
import biz.thonbecker.personal.booking.platform.web.model.CreateBookingTypeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for managing bookings, types, and availability.
 *
 * <p>All endpoints require authentication.
 */
@Controller
@RequestMapping("/booking/admin")
@RequiredArgsConstructor
@Slf4j
public class BookingAdminController {

    private final BookingFacade bookingFacade;

    /**
     * Admin dashboard page.
     *
     * @param model Spring MVC model
     * @return Thymeleaf template name
     */
    @GetMapping
    public String adminDashboard(final Model model) {
        final var bookingTypes = bookingFacade.getAllBookingTypes();
        final var bookings = bookingFacade.getAllBookings();
        final var availabilitySlots = bookingFacade.getAllAvailabilitySlots();

        model.addAttribute("bookingTypes", bookingTypes);
        model.addAttribute("bookings", bookings);
        model.addAttribute("availabilitySlots", availabilitySlots);

        return "booking/admin";
    }

    /**
     * Creates a new booking type.
     *
     * @param request Booking type details
     * @return Created booking type
     */
    @PostMapping("/types")
    @ResponseBody
    public ResponseEntity<BookingType> createBookingType(@Valid @RequestBody final CreateBookingTypeRequest request) {
        try {
            log.info("Creating booking type: {}", request.name());

            final var bookingType = bookingFacade.createBookingType(
                    request.name(),
                    request.description(),
                    request.durationMinutes(),
                    request.bufferMinutes(),
                    request.color());

            log.info("Successfully created booking type: {}", bookingType.id());
            return ResponseEntity.ok(bookingType);

        } catch (final Exception e) {
            log.error("Failed to create booking type: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Creates an availability slot.
     *
     * @param request Slot details
     * @return Success response
     */
    @PostMapping("/availability")
    @ResponseBody
    public ResponseEntity<Void> createAvailabilitySlot(
            @Valid @RequestBody final CreateAvailabilitySlotRequest request) {
        try {
            log.info("Creating availability slot: {} - {}", request.startTime(), request.endTime());

            bookingFacade.createAvailabilitySlot(request.startTime(), request.endTime());

            log.info("Successfully created availability slot");
            return ResponseEntity.ok().build();

        } catch (final Exception e) {
            log.error("Failed to create availability slot: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deletes an availability slot.
     *
     * @param slotId Slot identifier
     * @return Success response
     */
    @DeleteMapping("/availability/{slotId}")
    @ResponseBody
    public ResponseEntity<Void> deleteAvailabilitySlot(@PathVariable final Long slotId) {
        try {
            log.info("Deleting availability slot: {}", slotId);
            bookingFacade.deleteAvailabilitySlot(slotId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to delete availability slot: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lists all bookings (HTMX endpoint).
     *
     * @param model Spring MVC model
     * @return Thymeleaf fragment with bookings
     */
    @GetMapping("/bookings")
    public String listBookings(final Model model) {
        final var bookings = bookingFacade.getAllBookings();
        model.addAttribute("bookings", bookings);
        return "booking/admin-fragments :: bookings-list";
    }

    /**
     * Cancels a booking (admin action).
     *
     * @param bookingId Booking identifier
     * @return Success response
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelBooking(@PathVariable final Long bookingId) {
        try {
            log.info("Admin cancelling booking: {}", bookingId);
            bookingFacade.cancelBooking(bookingId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to cancel booking: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
