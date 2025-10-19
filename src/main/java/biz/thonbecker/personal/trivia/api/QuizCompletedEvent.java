package biz.thonbecker.personal.trivia.api;

import java.time.Instant;
import java.util.List;

/**
 * Domain event published when a quiz is completed.
 * Other modules can listen to this event to perform actions like recording statistics,
 * sending notifications, or updating leaderboards.
 */
public record QuizCompletedEvent(
        Long quizId,
        String title,
        String winnerId,
        String winnerName,
        int finalScore,
        List<PlayerResult> allPlayers,
        Instant completedAt) {
    public record PlayerResult(String playerId, String playerName, int score, int rank) {}
}
