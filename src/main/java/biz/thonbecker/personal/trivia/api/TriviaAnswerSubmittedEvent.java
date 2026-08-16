package biz.thonbecker.personal.trivia.api;

public record TriviaAnswerSubmittedEvent(String distinctId, Long quizId, Long questionId, boolean correct) {}
