package biz.thonbecker.personal.notification.platform;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

/**
 * Service for sending email notifications about bookings via AWS SES.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final CalendarService calendarService;
    private final SesClient sesClient;
    private final NotificationProperties properties;

    /**
     * Sends a booking confirmation email with .ics attachment to the attendee.
     */
    @Retryable(maxAttempts = 3)
    public void sendBookingConfirmation(final BookingCreatedEvent event) {
        final var sender = properties.sender();
        final var icsContent = calendarService.generateICalendar(event, sender);
        final var subject = "Booking Confirmation - " + event.bookingTypeName();

        final var body = String.format(
                """
                Hi %s,

                Your booking has been confirmed!

                Booking Details:
                  Type: %s
                  Date/Time: %s - %s
                  Confirmation Code: %s

                You can view or cancel your booking at:
                  https://thonbecker.biz/booking/confirmation/%s

                The meeting details are attached as a calendar file (.ics).

                Looking forward to speaking with you!

                Best regards,
                Thon Becker""",
                event.attendeeName(),
                event.bookingTypeName(),
                event.startTime(),
                event.endTime(),
                event.confirmationCode(),
                event.confirmationCode());

        if (!properties.enabled()) {
            log.info(
                    "Email disabled — would send confirmation to {} for booking {}",
                    event.attendeeEmail(),
                    event.confirmationCode());
            log.debug("Subject: {}\nBody:\n{}", subject, body);
            return;
        }

        sendEmailWithAttachment(sender, event.attendeeEmail(), subject, body, icsContent, event.confirmationCode());
        log.info(
                "Sent booking confirmation email to {} for booking {}",
                event.attendeeEmail(),
                event.confirmationCode());
    }

    /**
     * Sends a cancellation notification email to the attendee.
     */
    @Retryable(maxAttempts = 3)
    public void sendCancellationNotification(final BookingCancelledEvent event) {
        final var sender = properties.sender();
        final var subject = "Booking Cancelled - " + event.bookingTypeName();

        final var body = String.format(
                """
                Hi %s,

                Your booking has been cancelled.

                Cancelled Booking Details:
                  Type: %s
                  Date/Time: %s - %s
                  Confirmation Code: %s

                If you'd like to reschedule, please visit:
                  https://thonbecker.biz/booking

                Best regards,
                Thon Becker""",
                event.attendeeName(),
                event.bookingTypeName(),
                event.startTime(),
                event.endTime(),
                event.confirmationCode());

        if (!properties.enabled()) {
            log.info(
                    "Email disabled — would send cancellation to {} for booking {}",
                    event.attendeeEmail(),
                    event.confirmationCode());
            return;
        }

        sendPlainEmail(sender, event.attendeeEmail(), subject, body);
        log.info("Sent cancellation email to {} for booking {}", event.attendeeEmail(), event.confirmationCode());
    }

    private void sendEmailWithAttachment(
            final String from,
            final String to,
            final String subject,
            final String body,
            final String icsContent,
            final String confirmationCode) {
        try {
            final var session = Session.getDefaultInstance(new Properties());
            final var message = new MimeMessage(session);
            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(from, "Thon Becker"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            // Text body part
            final var textPart = new MimeBodyPart();
            textPart.setText(body, "UTF-8");

            // Calendar attachment
            final var calendarPart = new MimeBodyPart();
            calendarPart.setContent(icsContent, "text/calendar; charset=UTF-8; method=REQUEST");
            calendarPart.setFileName("booking-" + confirmationCode + ".ics");

            // Combine
            final var multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(calendarPart);
            message.setContent(multipart);

            sendRawEmail(message);
        } catch (final Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send booking confirmation email", e);
        }
    }

    private void sendPlainEmail(final String from, final String to, final String subject, final String body) {
        try {
            final var session = Session.getDefaultInstance(new Properties());
            final var message = new MimeMessage(session);
            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(from, "Thon Becker"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setText(body, "UTF-8");

            sendRawEmail(message);
        } catch (final Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendRawEmail(final MimeMessage message) throws Exception {
        final var outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);

        final var rawMessage = RawMessage.builder()
                .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray())))
                .build();

        sesClient.sendRawEmail(
                SendRawEmailRequest.builder().rawMessage(rawMessage).build());
    }
}
