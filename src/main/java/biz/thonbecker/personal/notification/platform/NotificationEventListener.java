package biz.thonbecker.personal.notification.platform;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.booking.api.BookingFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for notification-related events from other modules.
 *
 * <p>Listens for events across modules and sends appropriate notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class NotificationEventListener {

    private final BookingFacade bookingFacade;
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
            final var booking = bookingFacade.getBookingByConfirmationCode(event.confirmationCode());
            emailService.sendBookingConfirmation(booking);
            emailService.sendBookingNotificationToAdmin(booking);
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
            final var booking = bookingFacade.getBookingByConfirmationCode(event.confirmationCode());
            emailService.sendCancellationNotification(booking);
            log.info("Successfully sent cancellation notification for {}", event.confirmationCode());
        } catch (final Exception e) {
            log.error(
                    "Failed to send cancellation notification for {}: {}", event.confirmationCode(), e.getMessage(), e);
        }
    }
}
