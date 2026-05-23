package biz.thonbecker.personal.trivia.platform.web;

record JoinQuizRequest(Long quizId, String playerId, String playerName) {}
