package biz.thonbecker.personal.trivia.api;

import biz.thonbecker.personal.trivia.domain.QuizDifficulty;

import java.time.Instant;
import java.util.List;

/**
 * Domain event published when a quiz is started.
 */
public record QuizStartedEvent(
        Long quizId,
        String title,
        QuizDifficulty difficulty,
        int questionCount,
        List<String> playerIds,
        Instant startedAt) {}
