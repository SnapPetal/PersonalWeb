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
 * <p>Listens for booking events and sends appropriate email notifications.
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

    @TransactionalEventListener
    void onBookingCreated(final BookingCreatedEvent event) {
        log.info("Sending booking confirmation email for {}", event.confirmationCode());
        emailService.sendBookingConfirmation(event);
        log.info("Successfully sent booking confirmation for {}", event.confirmationCode());
    }

    @TransactionalEventListener
    void onBookingCancelled(final BookingCancelledEvent event) {
        log.info("Sending cancellation email for {}", event.confirmationCode());
        emailService.sendCancellationNotification(event);
        log.info("Successfully sent cancellation notification for {}", event.confirmationCode());
    }
}
