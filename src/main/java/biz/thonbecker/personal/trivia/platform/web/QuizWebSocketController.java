package biz.thonbecker.personal.trivia.platform.web;

import biz.thonbecker.personal.trivia.api.QuizPlayerState;
import biz.thonbecker.personal.trivia.api.QuizState;
import biz.thonbecker.personal.trivia.api.QuizSummary;
import biz.thonbecker.personal.trivia.domain.Player;
import biz.thonbecker.personal.trivia.domain.Quiz;
import biz.thonbecker.personal.trivia.domain.QuizDifficulty;
import biz.thonbecker.personal.trivia.platform.TriviaService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for trivia quiz operations.
 * This is internal infrastructure and should not be accessed directly by other modules.
 */
@Controller
@Slf4j
class QuizWebSocketController {

    private final TriviaService triviaService;
    private final SimpMessagingTemplate messagingTemplate;

    public QuizWebSocketController(TriviaService triviaService, SimpMessagingTemplate messagingTemplate) {
        this.triviaService = triviaService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/quiz/create/trivia")
    public void createTriviaQuiz(TriviaQuizRequest request) {
        log.info("Received trivia quiz creation request: {}", request);

        Quiz quiz = triviaService.createTriviaQuiz(
                request.title(),
                request.questionCount(),
                QuizDifficulty.valueOf(request.difficulty()),
                request.creatorId());

        // Broadcast quiz creation to all subscribers
        messagingTemplate.convertAndSend("/topic/quiz/created", new QuizSummary(quiz.getId(), quiz.getTitle()));
        log.info("Quiz created and broadcast: {}", quiz.getId());
    }

    @MessageMapping("/quiz/join")
    public void joinQuiz(JoinQuizRequest request) {
        log.info("Player {} joining quiz {}", request.playerName(), request.quizId());

        final var quizPlayer = new Player();
        quizPlayer.setId(request.playerId());
        quizPlayer.setName(request.playerName());

        triviaService.addPlayer(request.quizId(), quizPlayer);

        // Broadcast updated player list
        List<QuizPlayerState> players = triviaService.getPlayers(request.quizId()).stream()
                .map(player -> new QuizPlayerState(player.getId(), player.getName(), player.getScore()))
                .toList();
        messagingTemplate.convertAndSend("/topic/quiz/players", players);
        log.info("Player list updated for quiz {}: {} players", request.quizId(), players.size());
    }

    @MessageMapping("/quiz/start")
    public void startQuiz(StartQuizRequest request) {
        log.info("Player {} attempting to start quiz: {}", request.playerId(), request.quizId());

        try {
            QuizState state = triviaService.startQuiz(request.quizId(), request.playerId());

            if (state != null) {
                // Broadcast quiz state to all subscribers of this specific quiz
                messagingTemplate.convertAndSend("/topic/quiz/state/" + request.quizId(), state);
                log.info("Quiz {} started and state broadcast", request.quizId());
            } else {
                log.warn("Failed to start quiz: {}", request.quizId());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to start quiz {}: {}", request.quizId(), e.getMessage());
            // Could optionally send an error message back to the client here
        }
    }

    @MessageMapping("/quiz/submit")
    public void submitAnswer(AnswerSubmission submission) {
        log.info("Answer submitted for quiz {} by player {}", submission.quizId(), submission.playerId());

        QuizState state = triviaService.submitAnswer(
                submission.quizId(), submission.playerId(), submission.questionId(), submission.selectedOption());

        if (state != null) {
            // Broadcast updated state (with scores) to all players
            messagingTemplate.convertAndSend("/topic/quiz/state/" + submission.quizId(), state);
        }
    }

    @MessageMapping("/quiz/next")
    public void nextQuestion(NextQuestionRequest request) {
        log.info("Moving to next question for quiz: {}", request.quizId());

        QuizState state = triviaService.nextQuestion(request.quizId());

        if (state != null) {
            messagingTemplate.convertAndSend("/topic/quiz/state/" + request.quizId(), state);
            log.info("Quiz {} moved to next question", request.quizId());
        }
    }
}
