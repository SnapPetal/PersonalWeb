package solutions.thonbecker.personal.types.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmission {
    private Long quizId;
    private String playerId;
    private Long questionId;
    private int selectedOption;
    private String timestamp;
}
