package biz.thonbecker.personal.trivia.api;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a completed quiz result.
 * This is a public API type exposed to other modules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResult {
    private Long id;
    private String quizTitle;
    private Long quizId;
    private String playerName;
    private String playerId;
    private int score;
    private int totalQuestions;
    private int correctAnswers;
    private LocalDateTime completedAt;
    private boolean isWinner;
    private String difficulty;
}
