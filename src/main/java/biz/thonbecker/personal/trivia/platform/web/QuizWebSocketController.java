package biz.thonbecker.personal.trivia.platform.web;

import biz.thonbecker.personal.trivia.api.QuizState;
import biz.thonbecker.personal.trivia.domain.Player;
import biz.thonbecker.personal.trivia.domain.Quiz;
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
                request.getTitle(), request.getQuestionCount(), request.getDifficulty(), request.getCreatorId());

        // Broadcast quiz creation to all subscribers
        messagingTemplate.convertAndSend("/topic/quiz/created", quiz);
        log.info("Quiz created and broadcast: {}", quiz.getId());
    }

    @MessageMapping("/quiz/join")
    public void joinQuiz(JoinQuizRequest request) {
        log.info("Player {} joining quiz {}", request.getPlayer().getName(), request.getQuizId());

        triviaService.addPlayer(request.getQuizId(), request.getPlayer());

        // Broadcast updated player list
        List<Player> players = triviaService.getPlayers(request.getQuizId());
        messagingTemplate.convertAndSend("/topic/quiz/players", players);
        log.info("Player list updated for quiz {}: {} players", request.getQuizId(), players.size());
    }

    @MessageMapping("/quiz/start")
    public void startQuiz(StartQuizRequest request) {
        log.info("Player {} attempting to start quiz: {}", request.getPlayerId(), request.getQuizId());

        try {
            QuizState state = triviaService.startQuiz(request.getQuizId(), request.getPlayerId());

            if (state != null) {
                // Broadcast quiz state to all subscribers of this specific quiz
                messagingTemplate.convertAndSend("/topic/quiz/state/" + request.getQuizId(), state);
                log.info("Quiz {} started and state broadcast", request.getQuizId());
            } else {
                log.warn("Failed to start quiz: {}", request.getQuizId());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to start quiz {}: {}", request.getQuizId(), e.getMessage());
            // Could optionally send an error message back to the client here
        }
    }

    @MessageMapping("/quiz/submit")
    public void submitAnswer(AnswerSubmission submission) {
        log.info("Answer submitted for quiz {} by player {}", submission.getQuizId(), submission.getPlayerId());

        QuizState state = triviaService.submitAnswer(
                submission.getQuizId(),
                submission.getPlayerId(),
                submission.getQuestionId(),
                submission.getSelectedOption());

        if (state != null) {
            // Broadcast updated state (with scores) to all players
            messagingTemplate.convertAndSend("/topic/quiz/state/" + submission.getQuizId(), state);
        }
    }

    @MessageMapping("/quiz/next")
    public void nextQuestion(NextQuestionRequest request) {
        log.info("Moving to next question for quiz: {}", request.getQuizId());

        QuizState state = triviaService.nextQuestion(request.getQuizId());

        if (state != null) {
            messagingTemplate.convertAndSend("/topic/quiz/state/" + request.getQuizId(), state);
            log.info("Quiz {} moved to next question", request.getQuizId());
        }
    }
}
