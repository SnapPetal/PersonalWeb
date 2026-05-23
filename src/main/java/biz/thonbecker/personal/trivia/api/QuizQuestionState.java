package biz.thonbecker.personal.trivia.api;

import java.util.List;

/**
 * Public view of a quiz question with the answer hidden.
 */
public record QuizQuestionState(Long id, String questionText, List<String> options) {}
