package biz.thonbecker.personal.trivia.api;

import java.time.Instant;
import java.util.List;

/**
 * Domain event published when a quiz is started.
 */
public record QuizStartedEvent(
        Long quizId, String title, String difficulty, int questionCount, List<String> playerIds, Instant startedAt) {}
