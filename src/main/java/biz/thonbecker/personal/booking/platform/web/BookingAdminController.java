package biz.thonbecker.personal.booking.platform.web;

import biz.thonbecker.personal.booking.platform.BookingService;
import biz.thonbecker.personal.booking.platform.web.model.BookingAdminSnapshot;
import biz.thonbecker.personal.booking.platform.web.model.CreateAvailabilitySlotRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API used by the private Cloudflare OS booking administration surface. */
@RestController
@RequestMapping("/booking/admin/api")
@RequiredArgsConstructor
@Slf4j
public class BookingAdminController {

    private final BookingService bookingService;

    @GetMapping("/snapshot")
    public BookingAdminSnapshot snapshot() {
        return new BookingAdminSnapshot(
                bookingService.getAllBookingTypes(),
                bookingService.getAllBookings(),
                bookingService.getAllAvailabilitySlots());
    }

    @PostMapping("/availability")
    public ResponseEntity<Void> createAvailability(@Valid @RequestBody final CreateAvailabilitySlotRequest request) {
        try {
            bookingService.createAvailabilitySlot(request.startTime(), request.endTime());
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to create availability slot from Cloudflare OS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/availability/{slotId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable final Long slotId) {
        try {
            bookingService.deleteAvailabilitySlot(slotId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to delete availability slot from Cloudflare OS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable final Long bookingId) {
        try {
            bookingService.cancelBooking(bookingId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to cancel booking from Cloudflare OS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
