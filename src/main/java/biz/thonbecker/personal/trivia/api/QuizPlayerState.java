package biz.thonbecker.personal.trivia.api;

/**
 * Public view of a quiz player used in WebSocket state updates.
 */
public record QuizPlayerState(String id, String name, int score) {}
