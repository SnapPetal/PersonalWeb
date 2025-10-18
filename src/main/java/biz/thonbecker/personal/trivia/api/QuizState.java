package biz.thonbecker.personal.trivia.api;

import biz.thonbecker.personal.trivia.domain.Player;
import biz.thonbecker.personal.trivia.domain.Question;
import biz.thonbecker.personal.trivia.domain.QuizStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the current state of a quiz.
 * This is a public API type that can be shared with other modules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizState {
    private Long quizId;
    private QuizStatus status;
    private Question currentQuestion;
    private List<Player> players;
    private int currentQuestionNumber;
    private int totalQuestions;
}
