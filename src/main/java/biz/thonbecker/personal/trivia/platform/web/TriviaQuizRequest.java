package biz.thonbecker.personal.trivia.platform.web;

record TriviaQuizRequest(String title, int questionCount, String difficulty, String creatorId) {}
