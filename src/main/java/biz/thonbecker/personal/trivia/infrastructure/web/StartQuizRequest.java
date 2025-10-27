package biz.thonbecker.personal.trivia.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class StartQuizRequest {
    private Long quizId;
    private String playerId; // ID of the player attempting to start the quiz
}
