package biz.thonbecker.personal.notification.platform;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending email notifications about bookings.
 *
 * <p>Currently logs emails to console. In production, integrate with AWS SES.
 * Works with event data from shared events package.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final CalendarService calendarService;

    private static final String SENDER_EMAIL = "thon.becker@gmail.com";
    private static final String ADMIN_EMAIL = "thon.becker@gmail.com";

    /**
     * Sends a booking confirmation email to the attendee.
     *
     * @param event The booking created event
     */
    public void sendBookingConfirmation(final BookingCreatedEvent event) {
        final var icsContent = calendarService.generateICalendar(event, SENDER_EMAIL);

        log.info("=== BOOKING CONFIRMATION EMAIL ===");
        log.info("To: {}", event.attendeeEmail());
        log.info("Subject: Booking Confirmation - {}", event.bookingTypeName());
        log.info("Body:");
        log.info("Hi {},", event.attendeeName());
        log.info("");
        log.info("Your booking has been confirmed!");
        log.info("");
        log.info("Booking Details:");
        log.info("  Type: {}", event.bookingTypeName());
        log.info("  Date/Time: {} - {}", event.startTime(), event.endTime());
        log.info("  Confirmation Code: {}", event.confirmationCode());
        log.info("");
        log.info("You can view or cancel your booking at:");
        log.info("  https://thonbecker.com/booking/confirmation/{}", event.confirmationCode());
        log.info("");
        log.info("The meeting details are attached as a calendar file (.ics).");
        log.info("");
        log.info("Looking forward to speaking with you!");
        log.info("");
        log.info("Best regards,");
        log.info("Thon Becker");
        log.info("================================");
        log.debug("iCalendar attachment content:\n{}", icsContent);

        // TODO: Integrate with AWS SES to actually send email
        // final var request = SendEmailRequest.builder()
        //     .destination(d -> d.toAddresses(event.attendeeEmail()))
        //     .message(m -> m
        //         .subject(s -> s.data("Booking Confirmation - " + event.bookingTypeName()))
        //         .body(b -> b.text(t -> t.data(emailBody))))
        //     .source(SENDER_EMAIL)
        //     .build();
        // sesClient.sendEmail(request);
    }

    /**
     * Sends a booking notification to the admin.
     *
     * @param event The booking created event
     */
    public void sendBookingNotificationToAdmin(final BookingCreatedEvent event) {
        log.info("=== BOOKING NOTIFICATION TO ADMIN ===");
        log.info("To: {}", ADMIN_EMAIL);
        log.info("Subject: New Booking - {}", event.bookingTypeName());
        log.info("Body:");
        log.info("New booking received!");
        log.info("");
        log.info("Booking Details:");
        log.info("  Type: {}", event.bookingTypeName());
        log.info("  Date/Time: {} - {}", event.startTime(), event.endTime());
        log.info("  Attendee: {} ({})", event.attendeeName(), event.attendeeEmail());
        if (event.attendeePhone() != null && !event.attendeePhone().isBlank()) {
            log.info("  Phone: {}", event.attendeePhone());
        }
        log.info("  Confirmation Code: {}", event.confirmationCode());
        if (event.message() != null && !event.message().isBlank()) {
            log.info("");
            log.info("Message from attendee:");
            log.info("{}", event.message());
        }
        log.info("================================");

        // TODO: Integrate with AWS SES
    }

    /**
     * Sends a cancellation notification to the attendee.
     *
     * @param event The booking cancelled event
     */
    public void sendCancellationNotification(final BookingCancelledEvent event) {
        log.info("=== BOOKING CANCELLATION EMAIL ===");
        log.info("To: {}", event.attendeeEmail());
        log.info("Subject: Booking Cancelled - {}", event.bookingTypeName());
        log.info("Body:");
        log.info("Hi {},", event.attendeeName());
        log.info("");
        log.info("Your booking has been cancelled.");
        log.info("");
        log.info("Cancelled Booking Details:");
        log.info("  Type: {}", event.bookingTypeName());
        log.info("  Date/Time: {} - {}", event.startTime(), event.endTime());
        log.info("  Confirmation Code: {}", event.confirmationCode());
        log.info("");
        log.info("If you'd like to reschedule, please visit:");
        log.info("  https://thonbecker.com/booking");
        log.info("");
        log.info("Best regards,");
        log.info("Thon Becker");
        log.info("================================");

        // TODO: Integrate with AWS SES
    }
}
