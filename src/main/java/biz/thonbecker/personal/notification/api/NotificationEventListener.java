package biz.thonbecker.personal.notification.api;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.notification.platform.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for notification-related events from other modules.
 *
 * <p>Listens for events across modules and sends appropriate notifications.
 * Events contain all necessary data, so no callbacks to originating modules are needed.
 *
 * <p>Uses {@link TransactionalEventListener} so events are processed after the publishing
 * transaction commits. Spring Modulith's event publication registry persists events to the
 * database, ensuring guaranteed delivery — if a listener fails, the event can be replayed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class NotificationEventListener {

    private final EmailNotificationService emailService;

    /**
     * Handles booking creation events by sending confirmation emails.
     * Runs after the booking transaction commits to ensure the booking is persisted.
     *
     * @param event The booking created event
     */
    @TransactionalEventListener
    void onBookingCreated(final BookingCreatedEvent event) {
        log.info("Notification module handling BookingCreatedEvent for booking {}", event.confirmationCode());
        emailService.sendBookingConfirmation(event);
        emailService.sendBookingNotificationToAdmin(event);
        log.info("Successfully sent booking notifications for {}", event.confirmationCode());
    }

    /**
     * Handles booking cancellation events by sending cancellation emails.
     * Runs after the cancellation transaction commits.
     *
     * @param event The booking cancelled event
     */
    @TransactionalEventListener
    void onBookingCancelled(final BookingCancelledEvent event) {
        log.info("Notification module handling BookingCancelledEvent for booking {}", event.confirmationCode());
        emailService.sendCancellationNotification(event);
        log.info("Successfully sent cancellation notification for {}", event.confirmationCode());
    }
}
