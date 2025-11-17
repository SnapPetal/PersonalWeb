package biz.thonbecker.personal.tankgame.web;

import biz.thonbecker.personal.tankgame.application.ProgressionService;
import biz.thonbecker.personal.tankgame.application.TankGameService;
import biz.thonbecker.personal.tankgame.domain.GameState;
import biz.thonbecker.personal.tankgame.domain.PlayerInput;
import biz.thonbecker.personal.tankgame.domain.PlayerProgression;
import biz.thonbecker.personal.tankgame.domain.Tank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TankGameWebSocketController {

    private final TankGameService tankGameService;
    private final ProgressionService progressionService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/tankgame/create")
    public void createGame() {
        GameState game = tankGameService.createGame();
        log.info("Game created via WebSocket: {}", game.getGameId());
        messagingTemplate.convertAndSend("/topic/tankgame/lobby", game);
    }

    @MessageMapping("/tankgame/join/{gameId}")
    public void joinGame(@DestinationVariable String gameId, @Payload Map<String, String> payload) {
        try {
            String playerName = payload.get("playerName");
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Player";
            }

            Tank tank = tankGameService.joinGame(gameId, playerName);
            log.info("Player {} joined game {} as tank {}", playerName, gameId, tank.getId());

            // Broadcast tank join info to the game topic
            messagingTemplate.convertAndSend(
                    "/topic/tankgame/joined/" + gameId,
                    Map.of(
                            "tankId",
                            tank.getId(),
                            "gameId",
                            gameId,
                            "playerName",
                            playerName,
                            "color",
                            tank.getColor()));

            // Send initial progression data to the player
            PlayerProgression progression =
                    progressionService.getOrCreateProgression(playerName, playerName);
            messagingTemplate.convertAndSend(
                    "/topic/tankgame/progression/" + tank.getId(),
                    Map.of("progression", progression));
        } catch (Exception e) {
            log.error("Error joining game: {}", e.getMessage());
            messagingTemplate.convertAndSend("/topic/tankgame/error", e.getMessage());
        }
    }

    @MessageMapping("/tankgame/input/{gameId}/{tankId}")
    public void updateInput(
            @DestinationVariable String gameId,
            @DestinationVariable String tankId,
            @Payload PlayerInput input) {
        tankGameService.updateInput(tankId, input);
    }

    @MessageMapping("/tankgame/leave/{gameId}/{tankId}")
    public void leaveGame(@DestinationVariable String gameId, @DestinationVariable String tankId) {
        tankGameService.leaveGame(gameId, tankId);
        log.info("Tank {} left game {}", tankId, gameId);
    }
}
