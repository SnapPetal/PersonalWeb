package solutions.thonbecker.personal.trivia.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Quiz {
    private final Long id;
    private final String title;
    private final List<Question> questions;
    private final int timePerQuestionInSeconds;
    private final List<Player> players = new ArrayList<>();
    private QuizStatus status = QuizStatus.CREATED;
    private int currentQuestionIndex = -1;
    private QuizDifficulty difficulty;

    public Question getCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    public void nextQuestion() {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
        } else {
            status = QuizStatus.COMPLETED;
        }
    }
}
