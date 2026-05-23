package biz.thonbecker.personal.shared.platform.service;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.user.api.UserLoginEvent;
import biz.thonbecker.personal.user.api.UserProfileUpdatedEvent;
import biz.thonbecker.personal.user.api.UserRegisteredEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostHogEventListener {

    private final PostHogAnalyticsService postHogAnalyticsService;

    @EventListener
    @Async
    void onUserRegistered(final UserRegisteredEvent event) {
        postHogAnalyticsService.capture(
                event.email(),
                "user_registered",
                Map.of(
                        "user_id", event.userId(),
                        "username", event.username(),
                        "registered_at", event.registeredAt().toString()));
    }

    @EventListener
    @Async
    void onUserLogin(final UserLoginEvent event) {
        postHogAnalyticsService.capture(
                event.username(),
                "user_login",
                Map.of("user_id", event.userId(), "login_at", event.loginAt().toString()));
    }

    @EventListener
    @Async
    void onUserProfileUpdated(final UserProfileUpdatedEvent event) {
        postHogAnalyticsService.capture(
                event.userId(),
                "user_profile_updated",
                Map.of(
                        "display_name",
                        event.displayName(),
                        "updated_at",
                        event.updatedAt().toString()));
    }

    @EventListener
    @Async
    void onBookingCreated(final BookingCreatedEvent event) {
        postHogAnalyticsService.capture(
                event.attendeeEmail(),
                "booking_created",
                Map.of(
                        "booking_id", event.bookingId(),
                        "confirmation_code", event.confirmationCode(),
                        "booking_type", event.bookingTypeName(),
                        "attendee_name", event.attendeeName(),
                        "start_time", event.startTime().toString(),
                        "end_time", event.endTime().toString()));
    }

    @EventListener
    @Async
    void onBookingCancelled(final BookingCancelledEvent event) {
        postHogAnalyticsService.capture(
                event.attendeeEmail(),
                "booking_cancelled",
                Map.of(
                        "booking_id", event.bookingId(),
                        "confirmation_code", event.confirmationCode(),
                        "booking_type", event.bookingTypeName(),
                        "attendee_name", event.attendeeName(),
                        "start_time", event.startTime().toString(),
                        "end_time", event.endTime().toString()));
    }
}
