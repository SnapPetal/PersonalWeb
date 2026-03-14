package biz.thonbecker.personal.notification.api;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.notification.platform.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for notification-related events from other modules.
 *
 * <p>Listens for events across modules and sends appropriate notifications.
 * Events contain all necessary data, so no callbacks to originating modules are needed.
 *
 * <p>Events are defined in the shared.events package to avoid module coupling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class NotificationEventListener {

    private final EmailNotificationService emailService;

    /**
     * Handles booking creation events by sending confirmation emails.
     *
     * @param event The booking created event
     */
    @EventListener
    void onBookingCreated(final BookingCreatedEvent event) {
        log.info("Notification module handling BookingCreatedEvent for booking {}", event.confirmationCode());

        try {
            emailService.sendBookingConfirmation(event);
            emailService.sendBookingNotificationToAdmin(event);
            log.info("Successfully sent booking notifications for {}", event.confirmationCode());
        } catch (final Exception e) {
            log.error("Failed to send booking notifications for {}: {}", event.confirmationCode(), e.getMessage(), e);
        }
    }

    /**
     * Handles booking cancellation events by sending cancellation emails.
     *
     * @param event The booking cancelled event
     */
    @EventListener
    void onBookingCancelled(final BookingCancelledEvent event) {
        log.info("Notification module handling BookingCancelledEvent for booking {}", event.confirmationCode());

        try {
            emailService.sendCancellationNotification(event);
            log.info("Successfully sent cancellation notification for {}", event.confirmationCode());
        } catch (final Exception e) {
            log.error(
                    "Failed to send cancellation notification for {}: {}", event.confirmationCode(), e.getMessage(), e);
        }
    }
}
