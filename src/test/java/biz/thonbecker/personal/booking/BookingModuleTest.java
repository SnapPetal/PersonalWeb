package biz.thonbecker.personal.booking;

import static org.assertj.core.api.Assertions.assertThat;

import biz.thonbecker.personal.IntegrationTest;
import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.booking.platform.BookingService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.Scenario;

/**
 * Integration tests for the Booking module verifying event publication.
 *
 * <p>Uses Spring Modulith's {@link Scenario} API to verify that booking operations
 * publish the correct events with complete data for downstream consumers (notification module).
 */
@IntegrationTest
class BookingModuleTest {

    @Autowired
    private BookingService bookingService;

    @Test
    void publishesBookingCreatedEventWhenBookingIsCreated(Scenario scenario) {
        // Ensure we have a booking type and availability
        final var bookingType =
                bookingService.createBookingType("Test Consultation", "Test booking type", 30, 10, "#007bff");

        final var tomorrow = LocalDate.now().plusDays(1);
        bookingService.createAvailabilitySlot(tomorrow.atTime(9, 0), tomorrow.atTime(17, 0));

        final var startTime = tomorrow.atTime(10, 0);

        scenario.stimulate(() -> bookingService.createBooking(
                        bookingType.id(),
                        "John Doe",
                        "john@example.com",
                        "555-1234",
                        startTime,
                        "Looking forward to it",
                        "test-user"))
                .andWaitForEventOfType(BookingCreatedEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.attendeeName()).isEqualTo("John Doe");
                    assertThat(event.attendeeEmail()).isEqualTo("john@example.com");
                    assertThat(event.attendeePhone()).isEqualTo("555-1234");
                    assertThat(event.bookingTypeName()).isEqualTo("Test Consultation");
                    assertThat(event.startTime()).isEqualTo(startTime);
                    assertThat(event.endTime()).isEqualTo(startTime.plusMinutes(30));
                    assertThat(event.confirmationCode()).isNotBlank();
                    assertThat(event.message()).isEqualTo("Looking forward to it");
                    assertThat(event.bookingId()).isNotNull();
                });
    }

    @Test
    void publishesBookingCancelledEventWhenBookingIsCancelled(Scenario scenario) {
        // Setup: create booking type, availability, and a booking
        final var bookingType =
                bookingService.createBookingType("Cancel Test", "For cancellation testing", 30, 10, "#dc3545");

        final var dayAfterTomorrow = LocalDate.now().plusDays(2);
        bookingService.createAvailabilitySlot(dayAfterTomorrow.atTime(9, 0), dayAfterTomorrow.atTime(17, 0));

        final var startTime = dayAfterTomorrow.atTime(14, 0);
        final var booking = bookingService.createBooking(
                bookingType.id(), "Jane Smith", "jane@example.com", null, startTime, null, "test-user");

        scenario.stimulate(() -> bookingService.cancelBooking(booking.id()))
                .andWaitForEventOfType(BookingCancelledEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.bookingId()).isEqualTo(booking.id());
                    assertThat(event.confirmationCode()).isEqualTo(booking.confirmationCode());
                    assertThat(event.attendeeEmail()).isEqualTo("jane@example.com");
                    assertThat(event.attendeeName()).isEqualTo("Jane Smith");
                    assertThat(event.bookingTypeName()).isEqualTo("Cancel Test");
                    assertThat(event.startTime()).isEqualTo(startTime);
                });
    }

    @Test
    void bookingCreatedEventContainsAllRequiredFieldsForNotification(Scenario scenario) {
        final var bookingType =
                bookingService.createBookingType("Complete Data Test", "Verify all fields", 60, 15, "#28a745");

        final var futureDate = LocalDate.now().plusDays(3);
        bookingService.createAvailabilitySlot(futureDate.atTime(9, 0), futureDate.atTime(17, 0));

        final var startTime = futureDate.atTime(11, 0);

        scenario.stimulate(() -> bookingService.createBooking(
                        bookingType.id(),
                        "Test User",
                        "test@example.com",
                        "555-9999",
                        startTime,
                        "Please call me first",
                        "test-user"))
                .andWaitForEventOfType(BookingCreatedEvent.class)
                .toArriveAndVerify(event -> {
                    // Verify all fields that EmailNotificationService and CalendarService need
                    assertThat(event.bookingId()).isNotNull();
                    assertThat(event.confirmationCode()).hasSize(8);
                    assertThat(event.attendeeEmail()).contains("@");
                    assertThat(event.attendeeName()).isNotBlank();
                    assertThat(event.bookingTypeName()).isNotBlank();
                    assertThat(event.startTime()).isBefore(event.endTime());
                });
    }
}
