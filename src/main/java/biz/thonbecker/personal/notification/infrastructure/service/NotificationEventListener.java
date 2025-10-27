package biz.thonbecker.personal.notification.infrastructure.service;

import biz.thonbecker.personal.foosball.api.GameRecordedEvent;
import biz.thonbecker.personal.notification.api.NotificationFacade;
import biz.thonbecker.personal.notification.domain.NotificationChannel;
import biz.thonbecker.personal.trivia.api.QuizCompletedEvent;
import biz.thonbecker.personal.trivia.api.QuizStartedEvent;
import biz.thonbecker.personal.user.api.UserRegisteredEvent;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event listener that sends notifications in response to domain events from other modules.
 * This demonstrates cross-module integration through event-driven architecture.
 */
@Component
@Slf4j
class NotificationEventListener {

    private final NotificationFacade notificationFacade;

    public NotificationEventListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    /**
     * Send notification when a user registers.
     */
    @EventListener
    @Async
    void onUserRegistered(UserRegisteredEvent event) {
        log.info("Sending welcome notification to new user: {}", event.username());

        notificationFacade.sendNotification(
                event.userId(),
                "Welcome!",
                "Welcome to the platform, " + event.username() + "! Thanks for registering.",
                NotificationChannel.EMAIL);
    }

    /**
     * Send notification when a quiz is completed.
     */
    @EventListener
    @Async
    void onQuizCompleted(QuizCompletedEvent event) {
        log.info("Sending quiz completion notifications for quiz: {}", event.title());

        // Notify winner
        notificationFacade.sendNotification(
                event.winnerId(),
                "Quiz Victory!",
                String.format(
                        "Congratulations! You won '%s' with a score of %d points!",
                        event.title(), event.finalScore()),
                NotificationChannel.WEBSOCKET);

        // Notify all participants
        List<String> participantIds =
                event.allPlayers().stream().map(p -> p.playerId()).toList();

        notificationFacade.sendBulkNotification(
                participantIds,
                "Quiz Completed",
                String.format(
                        "The quiz '%s' has been completed. Winner: %s with %d points!",
                        event.title(), event.winnerName(), event.finalScore()),
                NotificationChannel.WEBSOCKET);
    }

    /**
     * Send notification when a quiz starts.
     */
    @EventListener
    @Async
    void onQuizStarted(QuizStartedEvent event) {
        log.info("Sending quiz start notifications for quiz: {}", event.title());

        notificationFacade.sendBulkNotification(
                event.playerIds(),
                "Quiz Started!",
                String.format(
                        "The quiz '%s' has started! Good luck with %d questions (%s difficulty).",
                        event.title(), event.questionCount(), event.difficulty()),
                NotificationChannel.WEBSOCKET);
    }

    /**
     * Send notification when a foosball game is recorded.
     */
    @EventListener
    @Async
    void onGameRecorded(GameRecordedEvent event) {
        log.info("Sending foosball game result notification");

        // In a real app, you'd look up player IDs from team names
        // For now, we'll just log it
        log.info(
                "Foosball game result: {} vs {} - Winner: {}",
                event.team1Name(),
                event.team2Name(),
                event.winnerTeamName());
    }
}
