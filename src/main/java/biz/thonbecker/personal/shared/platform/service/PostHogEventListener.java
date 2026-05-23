package biz.thonbecker.personal.shared.platform.service;

import biz.thonbecker.personal.booking.api.BookingCancelledEvent;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import biz.thonbecker.personal.foosball.api.GameRecordedEvent;
import biz.thonbecker.personal.foosball.api.PlayerCreatedEvent;
import biz.thonbecker.personal.trivia.api.PlayerJoinedQuizEvent;
import biz.thonbecker.personal.trivia.api.QuizCompletedEvent;
import biz.thonbecker.personal.trivia.api.QuizStartedEvent;
import biz.thonbecker.personal.user.api.UserLoginEvent;
import biz.thonbecker.personal.user.api.UserProfileUpdatedEvent;
import biz.thonbecker.personal.user.api.UserRegisteredEvent;
import java.util.LinkedHashMap;
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

    @EventListener
    @Async
    void onQuizStarted(final QuizStartedEvent event) {
        final var properties = new LinkedHashMap<String, Object>();
        properties.put("quiz_id", event.quizId());
        properties.put("title", event.title());
        properties.put("difficulty", event.difficulty());
        properties.put("question_count", event.questionCount());
        properties.put("player_count", event.playerIds().size());
        properties.put("started_at", event.startedAt().toString());

        postHogAnalyticsService.capture("quiz-" + event.quizId(), "quiz_started", properties);
    }

    @EventListener
    @Async
    void onQuizCompleted(final QuizCompletedEvent event) {
        final var properties = new LinkedHashMap<String, Object>();
        properties.put("quiz_id", event.quizId());
        properties.put("title", event.title());
        properties.put("winner_id", event.winnerId());
        properties.put("winner_name", event.winnerName());
        properties.put("final_score", event.finalScore());
        properties.put("player_count", event.allPlayers().size());
        properties.put("completed_at", event.completedAt().toString());

        postHogAnalyticsService.capture("quiz-" + event.quizId(), "quiz_completed", properties);
    }

    @EventListener
    @Async
    void onPlayerJoinedQuiz(final PlayerJoinedQuizEvent event) {
        postHogAnalyticsService.capture(
                event.playerId(),
                "player_joined_quiz",
                Map.of(
                        "quiz_id", event.quizId(),
                        "player_name", event.playerName(),
                        "joined_at", event.joinedAt().toString()));
    }

    @EventListener
    @Async
    void onGameRecorded(final GameRecordedEvent event) {
        final var properties = new LinkedHashMap<String, Object>();
        properties.put("game_id", event.gameId());
        properties.put("team1_name", event.team1Name());
        properties.put("team1_score", event.team1Score());
        properties.put("team2_name", event.team2Name());
        properties.put("team2_score", event.team2Score());
        properties.put("winner_team_name", event.winnerTeamName());
        properties.put("result", event.result());
        properties.put("recorded_at", event.recordedAt().toString());

        postHogAnalyticsService.capture("game-" + event.gameId(), "game_recorded", properties);
    }

    @EventListener
    @Async
    void onPlayerCreated(final PlayerCreatedEvent event) {
        postHogAnalyticsService.capture(
                event.playerId(),
                "player_created",
                Map.of(
                        "player_name",
                        event.playerName(),
                        "created_at",
                        event.createdAt().toString()));
    }
}
