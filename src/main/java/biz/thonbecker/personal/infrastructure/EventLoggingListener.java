package biz.thonbecker.personal.infrastructure;

import biz.thonbecker.personal.foosball.api.GameRecordedEvent;
import biz.thonbecker.personal.foosball.api.PlayerCreatedEvent;
import biz.thonbecker.personal.trivia.api.PlayerJoinedQuizEvent;
import biz.thonbecker.personal.trivia.api.QuizCompletedEvent;
import biz.thonbecker.personal.trivia.api.QuizStartedEvent;
import biz.thonbecker.personal.user.api.UserLoginEvent;
import biz.thonbecker.personal.user.api.UserProfileUpdatedEvent;
import biz.thonbecker.personal.user.api.UserRegisteredEvent;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Example event listener that demonstrates cross-module communication.
 * This listener logs all domain events from different modules, showing how
 * modules can react to events without direct dependencies on each other.
 *
 * This is located outside the module structure (in the root infrastructure package)
 * so it can depend on multiple modules without violating modulith boundaries.
 *
 * In a real application, you might use event listeners to:
 * - Update statistics across modules
 * - Send notifications
 * - Trigger workflows
 * - Maintain eventual consistency
 */
@Component
@Slf4j
class EventLoggingListener {

    /**
     * Listens to quiz completion events.
     * This demonstrates how cross-cutting concerns can react to module events.
     */
    @EventListener
    @Async
    void onQuizCompleted(QuizCompletedEvent event) {
        log.info(
                "📊 Quiz '{}' completed! Winner: {} with {} points",
                event.title(),
                event.winnerName(),
                event.finalScore());

        log.debug(
                "Full leaderboard for quiz {}: {} players",
                event.quizId(),
                event.allPlayers().size());
    }

    /**
     * Listens to quiz started events.
     */
    @EventListener
    @Async
    void onQuizStarted(QuizStartedEvent event) {
        log.info(
                "🎯 Quiz '{}' started with {} questions ({} difficulty) and {} players",
                event.title(),
                event.questionCount(),
                event.difficulty(),
                event.playerIds().size());
    }

    /**
     * Listens to player joined quiz events.
     */
    @EventListener
    @Async
    void onPlayerJoinedQuiz(PlayerJoinedQuizEvent event) {
        log.info("👤 Player {} joined quiz {}", event.playerName(), event.quizId());
    }

    /**
     * Listens to foosball game recorded events.
     */
    @EventListener
    @Async
    void onGameRecorded(GameRecordedEvent event) {
        log.info(
                "⚽ Foosball game recorded: {} ({}) vs {} ({}) - Winner: {}",
                event.team1Name(),
                event.team1Score(),
                event.team2Name(),
                event.team2Score(),
                event.winnerTeamName());
    }

    /**
     * Listens to foosball player created events.
     */
    @EventListener
    @Async
    void onPlayerCreated(PlayerCreatedEvent event) {
        log.info("🆕 New foosball player created: {}", event.playerName());
    }

    /**
     * Listens to user registered events.
     */
    @EventListener
    @Async
    void onUserRegistered(UserRegisteredEvent event) {
        log.info("👋 New user registered: {} ({})", event.username(), event.email());
    }

    /**
     * Listens to user login events.
     */
    @EventListener
    @Async
    void onUserLogin(UserLoginEvent event) {
        log.info("🔐 User logged in: {}", event.username());
    }

    /**
     * Listens to user profile updated events.
     */
    @EventListener
    @Async
    void onUserProfileUpdated(UserProfileUpdatedEvent event) {
        log.info("✏️ User profile updated: {}", event.displayName());
    }
}
