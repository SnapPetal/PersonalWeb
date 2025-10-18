package biz.thonbecker.personal.trivia.infrastructure.web;

import biz.thonbecker.personal.trivia.api.QuizState;
import biz.thonbecker.personal.trivia.api.TriviaFacade;
import biz.thonbecker.personal.trivia.domain.Player;
import biz.thonbecker.personal.trivia.domain.Quiz;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * WebSocket controller for trivia quiz operations.
 * This is internal infrastructure and should not be accessed directly by other modules.
 */
@Controller
@Slf4j
class QuizWebSocketController {

    private final TriviaFacade triviaFacade;
    private final SimpMessagingTemplate messagingTemplate;

    public QuizWebSocketController(
            TriviaFacade triviaFacade, SimpMessagingTemplate messagingTemplate) {
        this.triviaFacade = triviaFacade;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/quiz/create/trivia")
    public void createTriviaQuiz(TriviaQuizRequest request) {
        log.info("Received trivia quiz creation request: {}", request);

        Quiz quiz = triviaFacade.createTriviaQuiz(
                request.getTitle(), request.getQuestionCount(), request.getDifficulty());

        // Broadcast quiz creation to all subscribers
        messagingTemplate.convertAndSend("/topic/quiz/created", quiz);
        log.info("Quiz created and broadcast: {}", quiz.getId());
    }

    @MessageMapping("/quiz/join")
    public void joinQuiz(JoinQuizRequest request) {
        log.info("Player {} joining quiz {}", request.getPlayer().getName(), request.getQuizId());

        triviaFacade.addPlayer(request.getQuizId(), request.getPlayer());

        // Broadcast updated player list
        List<Player> players = triviaFacade.getPlayers(request.getQuizId());
        messagingTemplate.convertAndSend("/topic/quiz/players", players);
        log.info(
                "Player list updated for quiz {}: {} players", request.getQuizId(), players.size());
    }

    @MessageMapping("/quiz/start")
    public void startQuiz(StartQuizRequest request) {
        log.info("Starting quiz: {}", request.getQuizId());

        QuizState state = triviaFacade.startQuiz(request.getQuizId());

        if (state != null) {
            // Broadcast quiz state to all subscribers of this specific quiz
            messagingTemplate.convertAndSend("/topic/quiz/state/" + request.getQuizId(), state);
            log.info("Quiz {} started and state broadcast", request.getQuizId());
        } else {
            log.warn("Failed to start quiz: {}", request.getQuizId());
        }
    }

    @MessageMapping("/quiz/submit")
    public void submitAnswer(AnswerSubmission submission) {
        log.info(
                "Answer submitted for quiz {} by player {}",
                submission.getQuizId(),
                submission.getPlayerId());

        QuizState state = triviaFacade.submitAnswer(
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

        QuizState state = triviaFacade.nextQuestion(request.getQuizId());

        if (state != null) {
            messagingTemplate.convertAndSend("/topic/quiz/state/" + request.getQuizId(), state);
            log.info("Quiz {} moved to next question", request.getQuizId());
        }
    }
}
