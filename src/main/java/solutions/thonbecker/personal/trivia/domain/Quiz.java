package solutions.thonbecker.personal.trivia.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Quiz {
    private final Long id;
    private final String title;
    private final List<Question> questions;
    private final int timePerQuestionInSeconds;
    private final QuizDifficulty difficulty;
    private final List<Player> players = new ArrayList<>();
    private QuizStatus status = QuizStatus.CREATED;
    private int currentQuestionIndex = -1;

    public Quiz(
            Long id,
            String title,
            List<Question> questions,
            int timePerQuestionInSeconds,
            QuizDifficulty difficulty) {
        this.id = id;
        this.title = title;
        this.questions = questions;
        this.timePerQuestionInSeconds = timePerQuestionInSeconds;
        this.difficulty = difficulty;
    }

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
