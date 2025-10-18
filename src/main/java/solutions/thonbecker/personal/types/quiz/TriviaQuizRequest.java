package solutions.thonbecker.personal.types.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriviaQuizRequest {
    private String title;
    private int questionCount;
    private QuizDifficulty difficulty;
}
