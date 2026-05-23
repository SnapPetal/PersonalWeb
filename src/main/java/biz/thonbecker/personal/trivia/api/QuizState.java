package biz.thonbecker.personal.trivia.api;

import java.util.List;

/**
 * Represents the current state of a quiz.
 * This is a public API type that can be shared with other modules.
 */
public record QuizState(
        Long quizId,
        String status,
        QuizQuestionState currentQuestion,
        List<QuizPlayerState> players,
        int currentQuestionNumber,
        int totalQuestions) {}
