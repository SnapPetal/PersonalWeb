package biz.thonbecker.personal.booking.platform;

import biz.thonbecker.personal.booking.api.BookingCancellationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for cancellation requests from external modules.
 *
 * <p>When the calendar module detects a deleted calendar event, it publishes
 * a {@link BookingCancellationRequestedEvent}. This listener cancels the booking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class BookingCancellationListener {

    private final BookingService bookingService;

    @EventListener
    void onCancellationRequested(final BookingCancellationRequestedEvent event) {
        log.info("Cancellation requested for booking {}", event.bookingId());
        bookingService.cancelBooking(event.bookingId());
    }
}
