package solutions.thonbecker.personal.trivia.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import solutions.thonbecker.personal.trivia.domain.QuizDifficulty;

@Data
@NoArgsConstructor
@AllArgsConstructor
class TriviaQuizRequest {
    private String title;
    private int questionCount;
    private QuizDifficulty difficulty;
}
