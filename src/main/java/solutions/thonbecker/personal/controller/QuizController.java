package solutions.thonbecker.personal.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import solutions.thonbecker.personal.service.TriviaService;
import solutions.thonbecker.personal.types.quiz.*;

import java.util.List;

@Controller
@Slf4j
public class QuizController {

    private final TriviaService triviaService;
    private final SimpMessagingTemplate messagingTemplate;

    public QuizController(TriviaService triviaService, SimpMessagingTemplate messagingTemplate) {
        this.triviaService = triviaService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/quiz/create/trivia")
    public void createTriviaQuiz(TriviaQuizRequest request) {
        log.info("Received trivia quiz creation request: {}", request);

        Quiz quiz = triviaService.createTriviaQuiz(
                request.getTitle(), request.getQuestionCount(), request.getDifficulty());

        // Broadcast quiz creation to all subscribers
        messagingTemplate.convertAndSend("/topic/quiz/created", quiz);
        log.info("Quiz created and broadcast: {}", quiz.getId());
    }

    @MessageMapping("/quiz/join")
    public void joinQuiz(JoinQuizRequest request) {
        log.info("Player {} joining quiz {}", request.getPlayer().getName(), request.getQuizId());

        triviaService.addPlayer(request.getQuizId(), request.getPlayer());

        // Broadcast updated player list
        List<QuizPlayer> players = triviaService.getPlayers(request.getQuizId());
        messagingTemplate.convertAndSend("/topic/quiz/players", players);
        log.info(
                "Player list updated for quiz {}: {} players", request.getQuizId(), players.size());
    }

    @MessageMapping("/quiz/start")
    public void startQuiz(StartQuizRequest request) {
        log.info("Starting quiz: {}", request.getQuizId());

        QuizState state = triviaService.startQuiz(request.getQuizId());

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
    public void nextQuestion(StartQuizRequest request) {
        log.info("Moving to next question for quiz: {}", request.getQuizId());

        QuizState state = triviaService.nextQuestion(request.getQuizId());

        if (state != null) {
            messagingTemplate.convertAndSend("/topic/quiz/state/" + request.getQuizId(), state);
            log.info("Quiz {} moved to next question", request.getQuizId());
        }
    }
}
