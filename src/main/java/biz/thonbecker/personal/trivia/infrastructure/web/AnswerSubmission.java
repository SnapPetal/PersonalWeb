package biz.thonbecker.personal.trivia.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class AnswerSubmission {
    private Long quizId;
    private String playerId;
    private Long questionId;
    private int selectedOption;
    private String timestamp;
}
