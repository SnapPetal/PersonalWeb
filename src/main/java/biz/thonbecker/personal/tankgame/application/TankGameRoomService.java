package biz.thonbecker.personal.tankgame.application;

import biz.thonbecker.personal.tankgame.domain.GameState;
import biz.thonbecker.personal.tankgame.domain.PlayerInput;
import biz.thonbecker.personal.tankgame.domain.Tank;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TankGameRoomService {
    private static final String[] TANK_COLORS = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A"};
    private static final int MAX_PLAYERS_PER_GAME = 4;
    private static final long AI_MATCHMAKING_DELAY_MS = 5_000;

    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final Map<String, PlayerInput> playerInputs = new ConcurrentHashMap<>();
    private final Map<String, Long> gameStartTimes = new ConcurrentHashMap<>();

    public GameState createGame() {
        final var game = new GameState();
        activeGames.put(game.getGameId(), game);
        log.info("Created new tank game: {}", game.getGameId());
        return game;
    }

    public synchronized GameState findOrCreateWaitingGame() {
        return activeGames.values().stream()
                .filter(game -> game.getStatus() == GameState.GameStatus.WAITING)
                .filter(game -> game.getTanks().size() < MAX_PLAYERS_PER_GAME)
                .findFirst()
                .orElseGet(this::createGame);
    }

    public synchronized Tank joinGame(final String gameId, final String playerName) {
        final var game = activeGames.get(gameId);
        if (game == null) throw new IllegalArgumentException("Game not found: " + gameId);
        if (game.getTanks().size() >= MAX_PLAYERS_PER_GAME) throw new IllegalStateException("Game is full");

        final var tankCount = game.getTanks().size();
        final var spawn = findSpawnPosition(game, tankCount);
        final var tank = new Tank(
                UUID.randomUUID().toString(),
                playerName,
                spawn[0],
                spawn[1],
                TANK_COLORS[tankCount % TANK_COLORS.length]);
        game.addTank(tank);
        playerInputs.put(tank.getId(), new PlayerInput());
        if (game.getStatus() == GameState.GameStatus.PLAYING && !gameStartTimes.containsKey(gameId)) {
            gameStartTimes.put(gameId, System.currentTimeMillis());
            log.info("Game {} started with {} players", gameId, game.getTanks().size());
        }
        log.info("Player {} joined game {} as tank {}", playerName, gameId, tank.getId());
        return tank;
    }

    public void leaveGame(final String gameId, final String tankId) {
        final var game = activeGames.get(gameId);
        if (game == null) return;
        game.removeTank(tankId);
        playerInputs.remove(tankId);
        if (game.getTanks().isEmpty()) {
            activeGames.remove(gameId);
            gameStartTimes.remove(gameId);
            log.info("Game {} removed (no players)", gameId);
        }
    }

    public void updateInput(final String tankId, final PlayerInput input) {
        playerInputs.put(tankId, input);
    }

    public PlayerInput getInput(final String tankId) {
        return playerInputs.get(tankId);
    }

    public PlayerInput getOrCreateInput(final String tankId) {
        return playerInputs.computeIfAbsent(tankId, ignored -> new PlayerInput());
    }

    public void addAiOpponents(final long now) {
        activeGames.values().stream()
                .filter(game -> game.getStatus() == GameState.GameStatus.WAITING)
                .filter(game -> game.getTanks().values().stream().anyMatch(tank -> !tank.isBot()))
                .filter(game -> now - game.getCreatedAt() >= AI_MATCHMAKING_DELAY_MS)
                .forEach(game -> {
                    try {
                        final var bot = joinGame(game.getGameId(), "Arena Guard");
                        bot.setBot(true);
                    } catch (Exception e) {
                        log.warn("Could not add AI opponent to game {}: {}", game.getGameId(), e.getMessage());
                    }
                });
    }

    public GameState getGame(final String gameId) {
        return activeGames.get(gameId);
    }

    public Map<String, GameState> getActiveGames() {
        return new HashMap<>(activeGames);
    }

    public Long getGameStartTime(final String gameId) {
        return gameStartTimes.get(gameId);
    }

    public void removeGameStartTime(final String gameId) {
        gameStartTimes.remove(gameId);
    }

    private double[] findSpawnPosition(final GameState game, final int tankCount) {
        final double[][] spawnPoints = {
            {60, 60}, {game.getMapWidth() - 100, 60},
            {60, game.getMapHeight() - 100}, {game.getMapWidth() - 100, game.getMapHeight() - 100}
        };
        if (tankCount < spawnPoints.length) {
            final var spawn = spawnPoints[tankCount];
            final var tempTank = new Tank("temp", "temp", spawn[0], spawn[1], "");
            if (game.getWalls().stream().noneMatch(tempTank::collidesWith)) return spawn;
        }
        final var random = new Random();
        for (int attempt = 0; attempt < 50; attempt++) {
            final var x = 80 + random.nextDouble() * (game.getMapWidth() - 160);
            final var y = 80 + random.nextDouble() * (game.getMapHeight() - 160);
            final var tempTank = new Tank("temp", "temp", x, y, "");
            final var nearTank = game.getTanks().values().stream()
                    .anyMatch(other -> Math.hypot(other.getX() - x, other.getY() - y) < 200);
            if (game.getWalls().stream().noneMatch(tempTank::collidesWith) && !nearTank) return new double[] {x, y};
        }
        return new double[] {game.getMapWidth() / 2.0, game.getMapHeight() / 2.0};
    }
}
