package solutions.thonbecker.personal.trivia.infrastructure;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import solutions.thonbecker.personal.entity.QuizResultEntity;
import solutions.thonbecker.personal.repository.QuizResultRepository;
import solutions.thonbecker.personal.trivia.api.*;
import solutions.thonbecker.personal.trivia.domain.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Internal implementation of the Trivia Facade.
 * This class is NOT part of the public API and should not be used directly by other modules.
 */
@Service
@Slf4j
class TriviaFacadeImpl implements TriviaFacade {

    private static final int POINTS_PER_CORRECT_ANSWER = 100;
    private static final int DEFAULT_TIME_PER_QUESTION_SECONDS = 60;
    private static final int ANSWER_HIDDEN = -1;

    private final Map<Long, Quiz> quizzes = new ConcurrentHashMap<>();
    private final AtomicLong quizIdGenerator = new AtomicLong(System.currentTimeMillis());
    private final QuestionGenerator questionGenerator;
    private final QuizResultRepository quizResultRepository;

    public TriviaFacadeImpl(
            QuestionGenerator questionGenerator, QuizResultRepository quizResultRepository) {
        this.questionGenerator = questionGenerator;
        this.quizResultRepository = quizResultRepository;
    }

    @Override
    public Quiz createTriviaQuiz(String title, int questionCount, QuizDifficulty difficulty) {
        // Input validation
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz title cannot be null or empty");
        }
        if (questionCount <= 0 || questionCount > 20) {
            throw new IllegalArgumentException("Question count must be between 1 and 20");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty cannot be null");
        }

        log.info(
                "Creating Financial Peace trivia quiz: {} with {} questions at {} difficulty",
                title,
                questionCount,
                difficulty);

        Long quizId = quizIdGenerator.incrementAndGet();
        List<Question> questions = questionGenerator.generateQuestions(questionCount, difficulty);

        Quiz quiz = new Quiz(quizId, title, questions, DEFAULT_TIME_PER_QUESTION_SECONDS);
        quiz.setDifficulty(difficulty);
        quizzes.put(quizId, quiz);

        log.info(
                "Quiz created with ID: {} with {} AI-generated questions",
                quizId,
                questions.size());
        return quiz;
    }

    @Override
    public Optional<Quiz> getQuiz(Long quizId) {
        return Optional.ofNullable(quizzes.get(quizId));
    }

    @Override
    public void addPlayer(Long quizId, Player player) {
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null");
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (player.getId() == null || player.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }
        if (player.getName() == null || player.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }

        Quiz quiz = quizzes.get(quizId);
        if (quiz != null) {
            // Check if player already exists
            boolean playerExists =
                    quiz.getPlayers().stream().anyMatch(p -> p.getId().equals(player.getId()));

            if (!playerExists) {
                quiz.getPlayers().add(player);
                log.info("Player {} joined quiz {}", player.getName(), quizId);
            }
        }
    }

    @Override
    public List<Player> getPlayers(Long quizId) {
        Quiz quiz = quizzes.get(quizId);
        return quiz != null ? quiz.getPlayers() : Collections.emptyList();
    }

    @Override
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

    @Override
    public QuizState submitAnswer(
            Long quizId, String playerId, Long questionId, int selectedOption) {
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
                            player.setScore(player.getScore() + POINTS_PER_CORRECT_ANSWER);
                            log.info(
                                    "Player {} answered correctly! New score: {}",
                                    player.getName(),
                                    player.getScore());
                        });
            }
        }

        return buildQuizState(quiz);
    }

    @Override
    public QuizState nextQuestion(Long quizId) {
        Quiz quiz = quizzes.get(quizId);
        if (quiz == null) {
            return null;
        }

        quiz.nextQuestion();
        log.info(
                "Quiz {} moved to question {}/{}",
                quizId,
                quiz.getCurrentQuestionIndex() + 1,
                quiz.getQuestions().size());

        // Check if quiz is completed and save results
        if (quiz.getStatus() == QuizStatus.COMPLETED) {
            saveQuizResults(quiz);
        }

        return buildQuizState(quiz);
    }

    @Override
    public List<QuizResult> getWinners() {
        return quizResultRepository.findByIsWinnerTrueOrderByCompletedAtDesc().stream()
                .map(this::toQuizResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizResult> getPlayerHistory(String playerId) {
        return quizResultRepository.findByPlayerIdOrderByCompletedAtDesc(playerId).stream()
                .map(this::toQuizResult)
                .collect(Collectors.toList());
    }

    private void saveQuizResults(Quiz quiz) {
        log.info("Saving quiz results for quiz {}", quiz.getId());

        // Find the winner (highest score)
        Player winner = quiz.getPlayers().stream()
                .max(Comparator.comparingInt(Player::getScore))
                .orElse(null);

        for (Player player : quiz.getPlayers()) {
            int correctAnswers = player.getScore() / POINTS_PER_CORRECT_ANSWER;

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
                    quiz.getDifficulty() != null ? quiz.getDifficulty().name() : "MEDIUM");

            quizResultRepository.save(result);
            log.info(
                    "Saved quiz result for player {} with score {}",
                    player.getName(),
                    player.getScore());
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
                    ANSWER_HIDDEN);
        }

        return new QuizState(
                quiz.getId(),
                quiz.getStatus(),
                sanitizedQuestion,
                quiz.getPlayers(),
                quiz.getCurrentQuestionIndex(),
                quiz.getQuestions().size());
    }

    private QuizResult toQuizResult(QuizResultEntity entity) {
        return new QuizResult(
                entity.getId(),
                entity.getQuizTitle(),
                entity.getQuizId(),
                entity.getPlayerName(),
                entity.getPlayerId(),
                entity.getScore(),
                entity.getTotalQuestions(),
                entity.getCorrectAnswers(),
                entity.getCompletedAt(),
                entity.getIsWinner(),
                entity.getDifficulty());
    }
}
