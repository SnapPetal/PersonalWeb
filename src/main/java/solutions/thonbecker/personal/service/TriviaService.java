package solutions.thonbecker.personal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import solutions.thonbecker.personal.entity.QuizResultEntity;
import solutions.thonbecker.personal.repository.QuizResultRepository;
import solutions.thonbecker.personal.types.quiz.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class TriviaService {

    private final Map<Long, Quiz> quizzes = new ConcurrentHashMap<>();
    private final AtomicLong quizIdGenerator = new AtomicLong(System.currentTimeMillis());
    private final FinancialPeaceQuestionGenerator questionGenerator;
    private final QuizResultRepository quizResultRepository;

    public TriviaService(FinancialPeaceQuestionGenerator questionGenerator,
                         QuizResultRepository quizResultRepository) {
        this.questionGenerator = questionGenerator;
        this.quizResultRepository = quizResultRepository;
    }

    public Quiz createTriviaQuiz(String title, int questionCount, QuizDifficulty difficulty) {
        log.info("Creating Financial Peace trivia quiz: {} with {} questions at {} difficulty",
                title, questionCount, difficulty);

        Long quizId = quizIdGenerator.incrementAndGet();
        List<Question> questions = questionGenerator.generateQuestions(questionCount, difficulty);

        Quiz quiz = new Quiz(quizId, title, questions, 60);
        quiz.setDifficulty(difficulty);
        quizzes.put(quizId, quiz);

        log.info("Quiz created with ID: {} with {} AI-generated questions", quizId, questions.size());
        return quiz;
    }

    public Quiz getQuiz(Long quizId) {
        return quizzes.get(quizId);
    }

    public void addPlayer(Long quizId, QuizPlayer player) {
        Quiz quiz = quizzes.get(quizId);
        if (quiz != null) {
            // Check if player already exists
            boolean playerExists = quiz.getPlayers().stream()
                    .anyMatch(p -> p.getId().equals(player.getId()));

            if (!playerExists) {
                quiz.getPlayers().add(player);
                log.info("Player {} joined quiz {}", player.getName(), quizId);
            }
        }
    }

    public List<QuizPlayer> getPlayers(Long quizId) {
        Quiz quiz = quizzes.get(quizId);
        return quiz != null ? quiz.getPlayers() : Collections.emptyList();
    }

    public QuizState startQuiz(Long quizId) {
        Quiz quiz = quizzes.get(quizId);
        if (quiz == null) {
            log.warn("Quiz not found: {}", quizId);
            return null;
        }

        quiz.setStatus(QuizStatus.IN_PROGRESS);
        quiz.setCurrentQuestionIndex(0);
        log.info("Quiz {} started", quizId);

        return buildQuizState(quiz);
    }

    public QuizState submitAnswer(Long quizId, String playerId, Long questionId, int selectedOption) {
        Quiz quiz = quizzes.get(quizId);
        if (quiz == null) {
            return null;
        }

        Question currentQuestion = quiz.getCurrentQuestion();
        if (currentQuestion != null && currentQuestion.getId().equals(questionId)) {
            // Check if answer is correct
            if (selectedOption == currentQuestion.getCorrectAnswerIndex()) {
                // Award points to player
                quiz.getPlayers().stream()
                        .filter(p -> p.getId().equals(playerId))
                        .findFirst()
                        .ifPresent(player -> {
                            player.setScore(player.getScore() + 100);
                            log.info("Player {} answered correctly! New score: {}", player.getName(), player.getScore());
                        });
            }
        }

        return buildQuizState(quiz);
    }

    public QuizState nextQuestion(Long quizId) {
        Quiz quiz = quizzes.get(quizId);
        if (quiz == null) {
            return null;
        }

        quiz.nextQuestion();
        log.info("Quiz {} moved to question {}/{}", quizId, quiz.getCurrentQuestionIndex() + 1, quiz.getQuestions().size());

        // Check if quiz is completed and save results
        if (quiz.getStatus() == QuizStatus.COMPLETED) {
            saveQuizResults(quiz);
        }

        return buildQuizState(quiz);
    }

    private void saveQuizResults(Quiz quiz) {
        log.info("Saving quiz results for quiz {}", quiz.getId());

        // Find the winner (highest score)
        QuizPlayer winner = quiz.getPlayers().stream()
                .max(Comparator.comparingInt(QuizPlayer::getScore))
                .orElse(null);

        for (QuizPlayer player : quiz.getPlayers()) {
            int correctAnswers = player.getScore() / 100; // 100 points per correct answer

            QuizResultEntity result = new QuizResultEntity(
                    null,
                    quiz.getTitle(),
                    quiz.getId(),
                    player.getName(),
                    player.getId(),
                    player.getScore(),
                    quiz.getQuestions().size(),
                    correctAnswers,
                    LocalDateTime.now(),
                    winner != null && player.getId().equals(winner.getId()),
                    quiz.getDifficulty() != null ? quiz.getDifficulty().name() : "MEDIUM"
            );

            quizResultRepository.save(result);
            log.info("Saved quiz result for player {} with score {}", player.getName(), player.getScore());
        }
    }

    private QuizState buildQuizState(Quiz quiz) {
        Question currentQuestion = quiz.getCurrentQuestion();

        // Don't send the correct answer to clients
        Question sanitizedQuestion = null;
        if (currentQuestion != null) {
            sanitizedQuestion = new Question(
                    currentQuestion.getId(),
                    currentQuestion.getQuestionText(),
                    currentQuestion.getOptions(),
                    -1 // Hide correct answer from clients
            );
        }

        return new QuizState(
                quiz.getId(),
                quiz.getStatus(),
                sanitizedQuestion,
                quiz.getPlayers(),
                quiz.getCurrentQuestionIndex(),
                quiz.getQuestions().size()
        );
    }

    public List<QuizResultEntity> getWinners() {
        return quizResultRepository.findByIsWinnerTrueOrderByCompletedAtDesc();
    }

    public List<QuizResultEntity> getPlayerHistory(String playerId) {
        return quizResultRepository.findByPlayerIdOrderByCompletedAtDesc(playerId);
    }
}
