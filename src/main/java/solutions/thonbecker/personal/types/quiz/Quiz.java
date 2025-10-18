package solutions.thonbecker.personal.types.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    private Long id;
    private String title;
    private List<Question> questions;
    private int timePerQuestionInSeconds;
    private QuizStatus status;
    private List<QuizPlayer> players;
    private int currentQuestionIndex;
    private QuizDifficulty difficulty;

    public Quiz(Long id, String title, List<Question> questions, int timePerQuestionInSeconds) {
        this.id = id;
        this.title = title;
        this.questions = questions;
        this.timePerQuestionInSeconds = timePerQuestionInSeconds;
        this.status = QuizStatus.CREATED;
        this.players = new ArrayList<>();
        this.currentQuestionIndex = 0;
    }

    public Question getCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    public void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex >= questions.size()) {
            status = QuizStatus.COMPLETED;
        }
    }
}
