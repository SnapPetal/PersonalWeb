package solutions.thonbecker.personal.trivia.api;

import solutions.thonbecker.personal.trivia.domain.*;

import java.util.List;
import java.util.Optional;

/**
 * Public API for the Trivia module.
 * This is the only entry point other modules should use to interact with trivia functionality.
 */
public interface TriviaFacade {

    /**
     * Creates a new Financial Peace University trivia quiz with AI-generated questions
     */
    Quiz createTriviaQuiz(String title, int questionCount, QuizDifficulty difficulty);

    /**
     * Retrieves a quiz by its ID
     */
    Optional<Quiz> getQuiz(Long quizId);

    /**
     * Adds a player to a quiz
     */
    void addPlayer(Long quizId, Player player);

    /**
     * Gets all players in a quiz
     */
    List<Player> getPlayers(Long quizId);

    /**
     * Starts a quiz
     */
    QuizState startQuiz(Long quizId);

    /**
     * Submits an answer for a player
     */
    QuizState submitAnswer(Long quizId, String playerId, Long questionId, int selectedOption);

    /**
     * Moves to the next question
     */
    QuizState nextQuestion(Long quizId);

    /**
     * Gets quiz winners (completed quizzes)
     */
    List<QuizResult> getWinners();

    /**
     * Gets quiz history for a specific player
     */
    List<QuizResult> getPlayerHistory(String playerId);
}
