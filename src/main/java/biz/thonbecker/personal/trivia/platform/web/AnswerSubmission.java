package biz.thonbecker.personal.trivia.platform.web;

record AnswerSubmission(Long quizId, String playerId, Long questionId, int selectedOption, String timestamp) {}
