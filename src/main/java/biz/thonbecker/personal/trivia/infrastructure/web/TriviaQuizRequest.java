package biz.thonbecker.personal.trivia.infrastructure.web;

import biz.thonbecker.personal.trivia.domain.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class TriviaQuizRequest {
    private String title;
    private int questionCount;
    private QuizDifficulty difficulty;
    private String creatorId; // ID of the player creating the quiz
}
