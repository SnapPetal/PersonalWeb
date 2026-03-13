package biz.thonbecker.personal.notification.platform;

import biz.thonbecker.personal.booking.api.Booking;
import biz.thonbecker.personal.booking.platform.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending email notifications about bookings.
 *
 * <p>Currently logs emails to console. In production, integrate with AWS SES.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class EmailNotificationService {

    private final CalendarService calendarService;

    private static final String SENDER_EMAIL = "thon.becker@gmail.com";
    private static final String ADMIN_EMAIL = "thon.becker@gmail.com";

    /**
     * Sends a booking confirmation email to the attendee.
     *
     * @param booking The booking to send confirmation for
     */
    public void sendBookingConfirmation(final Booking booking) {
        final var icsContent = calendarService.generateICalendar(booking, SENDER_EMAIL);

        log.info("=== BOOKING CONFIRMATION EMAIL ===");
        log.info("To: {}", booking.attendeeEmail());
        log.info("Subject: Booking Confirmation - {}", booking.bookingType().name());
        log.info("Body:");
        log.info("Hi {},", booking.attendeeName());
        log.info("");
        log.info("Your booking has been confirmed!");
        log.info("");
        log.info("Booking Details:");
        log.info("  Type: {}", booking.bookingType().name());
        log.info("  Date/Time: {} - {}", booking.startTime(), booking.endTime());
        log.info("  Confirmation Code: {}", booking.confirmationCode());
        log.info("");
        log.info("You can view or cancel your booking at:");
        log.info("  https://thonbecker.com/booking/confirmation/{}", booking.confirmationCode());
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
        //     .destination(d -> d.toAddresses(booking.attendeeEmail()))
        //     .message(m -> m
        //         .subject(s -> s.data("Booking Confirmation - " + booking.bookingType().name()))
        //         .body(b -> b.text(t -> t.data(emailBody))))
        //     .source(SENDER_EMAIL)
        //     .build();
        // sesClient.sendEmail(request);
    }

    /**
     * Sends a booking notification to the admin.
     *
     * @param booking The booking to notify about
     */
    public void sendBookingNotificationToAdmin(final Booking booking) {
        log.info("=== BOOKING NOTIFICATION TO ADMIN ===");
        log.info("To: {}", ADMIN_EMAIL);
        log.info("Subject: New Booking - {}", booking.bookingType().name());
        log.info("Body:");
        log.info("New booking received!");
        log.info("");
        log.info("Booking Details:");
        log.info("  Type: {}", booking.bookingType().name());
        log.info("  Date/Time: {} - {}", booking.startTime(), booking.endTime());
        log.info("  Attendee: {} ({})", booking.attendeeName(), booking.attendeeEmail());
        if (booking.attendeePhone() != null && !booking.attendeePhone().isBlank()) {
            log.info("  Phone: {}", booking.attendeePhone());
        }
        log.info("  Confirmation Code: {}", booking.confirmationCode());
        if (booking.message() != null && !booking.message().isBlank()) {
            log.info("");
            log.info("Message from attendee:");
            log.info("{}", booking.message());
        }
        log.info("================================");

        // TODO: Integrate with AWS SES
    }

    /**
     * Sends a cancellation notification to the attendee.
     *
     * @param booking The cancelled booking
     */
    public void sendCancellationNotification(final Booking booking) {
        log.info("=== BOOKING CANCELLATION EMAIL ===");
        log.info("To: {}", booking.attendeeEmail());
        log.info("Subject: Booking Cancelled - {}", booking.bookingType().name());
        log.info("Body:");
        log.info("Hi {},", booking.attendeeName());
        log.info("");
        log.info("Your booking has been cancelled.");
        log.info("");
        log.info("Cancelled Booking Details:");
        log.info("  Type: {}", booking.bookingType().name());
        log.info("  Date/Time: {} - {}", booking.startTime(), booking.endTime());
        log.info("  Confirmation Code: {}", booking.confirmationCode());
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
