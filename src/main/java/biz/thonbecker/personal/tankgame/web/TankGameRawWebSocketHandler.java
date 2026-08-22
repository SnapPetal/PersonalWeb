package biz.thonbecker.personal.tankgame.web;

import biz.thonbecker.personal.tankgame.application.TankGameService;
import biz.thonbecker.personal.tankgame.domain.GameState;
import biz.thonbecker.personal.tankgame.domain.GameStateSnapshot;
import biz.thonbecker.personal.tankgame.domain.PlayerInput;
import biz.thonbecker.personal.tankgame.domain.Tank;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
class TankGameRawWebSocketHandler extends TextWebSocketHandler {

    private final TankGameService tankGameService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionGames = new ConcurrentHashMap<>();
    private final Map<String, String> sessionTanks = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastInputTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> lastInputSequences = new ConcurrentHashMap<>();
    private static final long MIN_INPUT_INTERVAL_MS = 20;

    TankGameRawWebSocketHandler(final TankGameService tankGameService, final ObjectMapper objectMapper) {
        this.tankGameService = tankGameService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        sessionLocks.put(session.getId(), new Object());
        send(session, Map.of("type", "connected", "message", "Ironbound Online connected"));
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        final JsonNode request = objectMapper.readTree(message.getPayload());
        final String action = request.path("action").asText();

        switch (action) {
            case "queue" ->
                joinQueue(
                        session,
                        request.path("playerName").asText("Player"),
                        request.path("loadoutId").asText("striker"));
            case "input" -> updateInput(session, request);
            case "leave" -> leave(session);
            default -> send(session, Map.of("type", "error", "message", "Unknown action: " + action));
        }
    }

    @Override
    public void afterConnectionClosed(
            final WebSocketSession session, final org.springframework.web.socket.CloseStatus status) {
        leave(session);
        sessions.remove(session.getId());
        sessionLocks.remove(session.getId());
        lastInputTimes.remove(session.getId());
        lastInputSequences.remove(session.getId());
    }

    private void joinQueue(final WebSocketSession session, final String playerName, final String loadoutId)
            throws Exception {
        leave(session);
        final GameState game = tankGameService.findOrCreateWaitingGame();
        final Tank tank = tankGameService.joinGame(game.getGameId(), playerName, loadoutId);
        sessionGames.put(session.getId(), game.getGameId());
        sessionTanks.put(session.getId(), tank.getId());
        send(session, Map.of("type", "joined", "gameId", game.getGameId(), "tankId", tank.getId()));
    }

    private void updateInput(final WebSocketSession session, final JsonNode request) throws Exception {
        final String tankId = sessionTanks.get(session.getId());
        final String gameId = sessionGames.get(session.getId());
        if (tankId == null) {
            return;
        }
        if (!gameId.equals(request.path("gameId").asText())) return;
        final long sequence = request.path("sequence").asLong(-1);
        final long previousSequence = lastInputSequences.getOrDefault(session.getId(), -1L);
        if (sequence <= previousSequence) return;
        lastInputSequences.put(session.getId(), sequence);
        final long now = System.currentTimeMillis();
        final long previous = lastInputTimes.getOrDefault(session.getId(), 0L);
        if (now - previous < MIN_INPUT_INTERVAL_MS) return;
        lastInputTimes.put(session.getId(), now);
        final var inputNode = request.path("input");
        final var input = new PlayerInput();
        input.setUp(inputNode.path("up").asBoolean());
        input.setDown(inputNode.path("down").asBoolean());
        input.setLeft(inputNode.path("left").asBoolean());
        input.setRight(inputNode.path("right").asBoolean());
        input.setShoot(inputNode.path("shoot").asBoolean());
        input.setMouseX(inputNode.path("mouseX").asDouble());
        input.setMouseY(inputNode.path("mouseY").asDouble());
        tankGameService.updateInput(tankId, input);
    }

    private void leave(final WebSocketSession session) {
        final String gameId = sessionGames.remove(session.getId());
        final String tankId = sessionTanks.remove(session.getId());
        lastInputTimes.remove(session.getId());
        lastInputSequences.remove(session.getId());
        if (gameId != null && tankId != null) {
            tankGameService.leaveGame(gameId, tankId);
        }
    }

    @Scheduled(fixedRate = 50)
    void broadcastStates() {
        sessions.values().forEach(session -> {
            final String gameId = sessionGames.get(session.getId());
            if (gameId == null || !session.isOpen()) {
                return;
            }
            final GameState game = tankGameService.getGame(gameId);
            if (game == null) {
                return;
            }
            try {
                synchronized (game) {
                    send(session, Map.of("type", "state", "game", GameStateSnapshot.from(game)));
                }
            } catch (Exception e) {
                log.debug("Could not send tank game state to {}: {}", session.getId(), e.getMessage());
            }
        });
    }

    private void send(final WebSocketSession session, final Object payload) throws Exception {
        final var lock = sessionLocks.computeIfAbsent(session.getId(), ignored -> new Object());
        synchronized (lock) {
            if (session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }
}
