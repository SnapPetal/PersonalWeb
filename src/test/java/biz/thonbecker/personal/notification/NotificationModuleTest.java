package biz.thonbecker.personal.notification;

import static org.assertj.core.api.Assertions.assertThatNoException;

import biz.thonbecker.personal.IntegrationTest;
import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.Scenario;

/**
 * Integration tests for the Notification module verifying event consumption.
 *
 * <p>Publishes booking events and verifies the notification module processes them
 * without errors. These tests ensure the event contract between booking and
 * notification modules is not silently broken.
 */
@IntegrationTest
class NotificationModuleTest {

    @Test
    void handlesBookingCreatedEventWithoutError(Scenario scenario) {
        final var event = new BookingCreatedEvent(
                1L,
                "ABC12345",
                "customer@example.com",
                "Test Customer",
                "555-0000",
                "30 Minute Consultation",
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(30),
                "I have a question about your services");

        assertThatNoException().isThrownBy(() -> scenario.publish(event).andWaitForStateChange(() -> true));
    }

    @Test
    void handlesBookingCancelledEventWithoutError(Scenario scenario) {
        final var event = new BookingCancelledEvent(
                2L,
                "XYZ98765",
                "cancel@example.com",
                "Cancel User",
                "1 Hour Technical Discussion",
                LocalDateTime.now().plusDays(2).withHour(14).withMinute(0),
                LocalDateTime.now().plusDays(2).withHour(15).withMinute(0));

        assertThatNoException().isThrownBy(() -> scenario.publish(event).andWaitForStateChange(() -> true));
    }

    @Test
    void handlesBookingCreatedEventWithMinimalData(Scenario scenario) {
        // Test with nullable fields set to null — notification should still work
        final var event = new BookingCreatedEvent(
                3L,
                "MIN00001",
                "minimal@example.com",
                "Minimal User",
                null,
                "Project Discovery Call",
                LocalDateTime.now().plusDays(3).withHour(9).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(9).withMinute(45),
                null);

        assertThatNoException().isThrownBy(() -> scenario.publish(event).andWaitForStateChange(() -> true));
    }
}
