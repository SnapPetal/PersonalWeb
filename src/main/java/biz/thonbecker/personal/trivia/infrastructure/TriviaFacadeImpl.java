package biz.thonbecker.personal.trivia.infrastructure;

import biz.thonbecker.personal.trivia.api.*;
import biz.thonbecker.personal.trivia.api.QuizResult;
import biz.thonbecker.personal.trivia.api.QuizState;
import biz.thonbecker.personal.trivia.api.TriviaFacade;
import biz.thonbecker.personal.trivia.domain.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;

    public TriviaFacadeImpl(
            QuestionGenerator questionGenerator,
            QuizResultRepository quizResultRepository,
            ApplicationEventPublisher eventPublisher) {
        this.questionGenerator = questionGenerator;
        this.quizResultRepository = quizResultRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Quiz createTriviaQuiz(String title, int questionCount, QuizDifficulty difficulty, String creatorId) {
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
        if (creatorId == null || creatorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Creator ID cannot be null or empty");
        }

        log.info(
                "Creating Financial Peace trivia quiz: {} with {} questions at {} difficulty by creator {}",
                title,
                questionCount,
                difficulty,
                creatorId);

        Long quizId = quizIdGenerator.incrementAndGet();
        List<Question> questions = questionGenerator.generateQuestions(questionCount, difficulty);

        Quiz quiz = new Quiz(quizId, title, questions, DEFAULT_TIME_PER_QUESTION_SECONDS, difficulty);
        quiz.setCreatorId(creatorId);
        quizzes.put(quizId, quiz);

        log.info("Quiz created with ID: {} with {} AI-generated questions", quizId, questions.size());
        return quiz;
    }

    @Override
    public Optional<Quiz> getQuiz(Long quizId) {
        return Optional.ofNullable(quizzes.get(quizId));
    }

    @Override
    @Transactional
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

                // Publish event
                eventPublisher.publishEvent(
                        new PlayerJoinedQuizEvent(quizId, player.getId(), player.getName(), Instant.now()));
            }
        }
    }

    @Override
    public List<Player> getPlayers(Long quizId) {
        Quiz quiz = quizzes.get(quizId);
        return quiz != null ? quiz.getPlayers() : Collections.emptyList();
    }

    @Override
    @Transactional
    public QuizState startQuiz(Long quizId, String playerId) {
        Quiz quiz = quizzes.get(quizId);
        if (quiz == null) {
            log.warn("Quiz not found: {}", quizId);
            return null;
        }

        // Validate that only the creator can start the quiz
        if (quiz.getCreatorId() == null || !quiz.getCreatorId().equals(playerId)) {
            log.warn(
                    "Player {} attempted to start quiz {} but is not the creator (creator: {})",
                    playerId,
                    quizId,
                    quiz.getCreatorId());
            throw new IllegalArgumentException("Only the quiz creator can start the quiz");
        }

        quiz.setStatus(QuizStatus.IN_PROGRESS);
        quiz.setCurrentQuestionIndex(0);
        log.info("Quiz {} started by creator {}", quizId, playerId);

        // Publish event
        eventPublisher.publishEvent(new QuizStartedEvent(
                quizId,
                quiz.getTitle(),
                quiz.getDifficulty(),
                quiz.getQuestions().size(),
                quiz.getPlayers().stream().map(Player::getId).toList(),
                Instant.now()));

        return buildQuizState(quiz);
    }

    @Override
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
                            player.setScore(player.getScore() + POINTS_PER_CORRECT_ANSWER);
                            log.info(
                                    "Player {} answered correctly! New score: {}", player.getName(), player.getScore());
                        });
            }
        }

        return buildQuizState(quiz);
    }

    @Override
    @Transactional
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
            publishQuizCompletedEvent(quiz);
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
                    player.getId().equals(winner.getId()),
                    quiz.getDifficulty() != null ? quiz.getDifficulty().name() : "MEDIUM");

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

    private void publishQuizCompletedEvent(Quiz quiz) {
        Player winner = quiz.getPlayers().stream()
                .max(Comparator.comparingInt(Player::getScore))
                .orElse(null);

        if (winner == null) {
            return;
        }

        // Create sorted player results
        List<Player> sortedPlayers = quiz.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getScore).reversed())
                .toList();

        List<QuizCompletedEvent.PlayerResult> playerResults = new ArrayList<>();
        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player p = sortedPlayers.get(i);
            playerResults.add(new QuizCompletedEvent.PlayerResult(
                    p.getId(), p.getName(), p.getScore(), i + 1 // rank (1-based)
                    ));
        }

        eventPublisher.publishEvent(new QuizCompletedEvent(
                quiz.getId(),
                quiz.getTitle(),
                winner.getId(),
                winner.getName(),
                winner.getScore(),
                playerResults,
                Instant.now()));

        log.info("Published QuizCompletedEvent for quiz {} with winner {}", quiz.getId(), winner.getName());
    }
}
