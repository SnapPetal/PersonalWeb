package solutions.thonbecker.personal.types.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizState {
    private Long quizId;
    private QuizStatus status;
    private Question currentQuestion;
    private List<QuizPlayer> players;
    private int currentQuestionIndex;
    private int totalQuestions;
}
