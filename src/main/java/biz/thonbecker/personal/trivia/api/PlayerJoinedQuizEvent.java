package biz.thonbecker.personal.trivia.api;

import java.time.Instant;

/**
 * Domain event published when a player joins a quiz.
 */
public record PlayerJoinedQuizEvent(
        Long quizId, String playerId, String playerName, Instant joinedAt) {}
