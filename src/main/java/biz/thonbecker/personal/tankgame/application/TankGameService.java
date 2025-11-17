package biz.thonbecker.personal.tankgame.application;

import biz.thonbecker.personal.tankgame.domain.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TankGameService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ProgressionService progressionService;
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final Map<String, PlayerInput> playerInputs = new ConcurrentHashMap<>();
    private final Map<String, Long> gameStartTimes = new ConcurrentHashMap<>();
    private long lastUpdateTime = System.currentTimeMillis();

    private static final String[] TANK_COLORS = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A"};
    private static final int MAX_PLAYERS_PER_GAME = 4;

    public TankGameService(
            SimpMessagingTemplate messagingTemplate, ProgressionService progressionService) {
        this.messagingTemplate = messagingTemplate;
        this.progressionService = progressionService;
    }

    public GameState createGame() {
        GameState game = new GameState();
        activeGames.put(game.getGameId(), game);
        log.info("Created new tank game: {}", game.getGameId());
        return game;
    }

    public synchronized Tank joinGame(String gameId, String playerName) {
        GameState game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        if (game.getTanks().size() >= MAX_PLAYERS_PER_GAME) {
            throw new IllegalStateException("Game is full");
        }

        String tankId = UUID.randomUUID().toString();
        int currentTankCount = game.getTanks().size();
        String color = TANK_COLORS[currentTankCount % TANK_COLORS.length];

        // Spawn position (pass current count to avoid race condition)
        double[] spawnPos = findSpawnPosition(game, currentTankCount);
        Tank tank = new Tank(tankId, playerName, spawnPos[0], spawnPos[1], color);

        game.addTank(tank);
        playerInputs.put(tankId, new PlayerInput());

        // Track game start time when status changes to PLAYING
        if (game.getStatus() == GameState.GameStatus.PLAYING
                && !gameStartTimes.containsKey(gameId)) {
            gameStartTimes.put(gameId, System.currentTimeMillis());
            log.info("Game {} started with {} players", gameId, game.getTanks().size());
        }

        log.info("Player {} joined game {} as tank {}", playerName, gameId, tankId);
        broadcastGameState(game);

        return tank;
    }

    public void leaveGame(String gameId, String tankId) {
        GameState game = activeGames.get(gameId);
        if (game != null) {
            game.removeTank(tankId);
            playerInputs.remove(tankId);
            log.info("Tank {} left game {}", tankId, gameId);

            if (game.getTanks().isEmpty()) {
                activeGames.remove(gameId);
                log.info("Game {} removed (no players)", gameId);
            } else {
                broadcastGameState(game);
            }
        }
    }

    public void updateInput(String tankId, PlayerInput input) {
        playerInputs.put(tankId, input);
    }

    @Scheduled(fixedRate = 16) // ~60 FPS
    public void gameLoop() {
        long now = System.currentTimeMillis();
        double rawDeltaTime = (now - lastUpdateTime) / 1000.0;
        lastUpdateTime = now;

        // Limit deltaTime to prevent huge jumps
        final double deltaTime = Math.min(rawDeltaTime, 0.1);

        activeGames.values().forEach(game -> {
            if (game.getStatus() != GameState.GameStatus.PLAYING) {
                return;
            }

            updateGame(game, deltaTime);
            broadcastGameState(game);
        });
    }

    private void updateGame(GameState game, double deltaTime) {
        // Update tanks based on input
        game.getTanks().values().forEach(tank -> {
            if (!tank.isAlive()) return;

            PlayerInput input = playerInputs.get(tank.getId());
            if (input == null) return;

            // Store old position for collision rollback
            double oldX = tank.getX();
            double oldY = tank.getY();

            // Update rotation based on mouse position
            double dx = input.getMouseX() - tank.getX();
            double dy = input.getMouseY() - tank.getY();
            tank.setRotation(Math.atan2(dy, dx));

            // Move tank
            tank.move(input, deltaTime);

            // Check collisions with walls
            boolean wallCollision =
                    game.getWalls().stream().anyMatch(wall -> tank.collidesWith(wall));

            // Check collisions with other tanks
            boolean tankCollision = game.getTanks().values().stream()
                    .filter(other -> !other.getId().equals(tank.getId()))
                    .filter(Tank::isAlive)
                    .anyMatch(other -> tank.collidesWith(other));

            // Rollback if collision
            if (wallCollision || tankCollision) {
                tank.setX(oldX);
                tank.setY(oldY);
            }

            // Handle shooting
            if (input.isShoot() && tank.canShoot()) {
                Projectile projectile = new Projectile(
                        UUID.randomUUID().toString(),
                        tank.getId(),
                        tank.getX() + tank.getWidth() / 2,
                        tank.getY() + tank.getHeight() / 2,
                        tank.getRotation());
                game.addProjectile(projectile);
                tank.recordShot();
            }
        });

        // Update projectiles
        Iterator<Projectile> projectileIterator = game.getProjectiles().iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();

            if (!projectile.isActive()) {
                projectileIterator.remove();
                continue;
            }

            projectile.update(deltaTime);

            // Check if out of bounds
            if (projectile.getX() < 0
                    || projectile.getX() > game.getMapWidth()
                    || projectile.getY() < 0
                    || projectile.getY() > game.getMapHeight()) {
                projectileIterator.remove();
                continue;
            }

            // Check wall collision
            boolean hitWall =
                    game.getWalls().stream().anyMatch(wall -> projectile.collidesWith(wall));

            if (hitWall) {
                projectileIterator.remove();
                continue;
            }

            // Check tank collision
            for (Tank tank : game.getTanks().values()) {
                if (tank.getId().equals(projectile.getOwnerId())) continue;
                if (!tank.isAlive()) continue;

                if (projectile.collidesWith(tank)) {
                    Tank shooter = game.getTanks().get(projectile.getOwnerId());
                    tank.takeDamage(projectile.getDamage(), shooter);
                    projectile.deactivate();

                    if (!tank.isAlive()) {
                        if (shooter != null) {
                            shooter.addKill();
                        }
                        log.info(
                                "Tank {} killed by {}",
                                tank.getPlayerName(),
                                shooter != null ? shooter.getPlayerName() : "unknown");
                    }

                    projectileIterator.remove();
                    break;
                }
            }
        }

        // Check if game is over and record results
        GameState.GameStatus previousStatus = game.getStatus();
        game.checkGameOver();

        // If game just finished, record match results
        if (previousStatus == GameState.GameStatus.PLAYING
                && game.getStatus() == GameState.GameStatus.FINISHED) {
            recordMatchResults(game);
        }
    }

    private void broadcastGameState(GameState game) {
        messagingTemplate.convertAndSend("/topic/tankgame/" + game.getGameId(), game);
    }

    private double[] findSpawnPosition(GameState game, int tankCount) {

        // Predefined spawn points in corners and edges (far apart)
        double[][] spawnPoints = {
            {60, 60}, // Top-left
            {game.getMapWidth() - 100, 60}, // Top-right
            {60, game.getMapHeight() - 100}, // Bottom-left
            {game.getMapWidth() - 100, game.getMapHeight() - 100} // Bottom-right
        };

        // Use predefined spawn points first (guarantees distance)
        if (tankCount < spawnPoints.length) {
            double[] spawn = spawnPoints[tankCount];
            Tank tempTank = new Tank("temp", "temp", spawn[0], spawn[1], "");

            // Verify not colliding with walls
            boolean wallCollision =
                    game.getWalls().stream().anyMatch(wall -> tempTank.collidesWith(wall));

            if (!wallCollision) {
                return spawn;
            }
        }

        // Fallback to random position with larger minimum distance
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = 50;

        while (attempts < maxAttempts) {
            double x = 80 + random.nextDouble() * (game.getMapWidth() - 160);
            double y = 80 + random.nextDouble() * (game.getMapHeight() - 160);

            Tank tempTank = new Tank("temp", "temp", x, y, "");

            // Check if position collides with walls
            boolean wallCollision =
                    game.getWalls().stream().anyMatch(wall -> tempTank.collidesWith(wall));

            // Check if too close to other tanks (increased from 100 to 200)
            boolean tankCollision = game.getTanks().values().stream().anyMatch(other -> {
                double distance =
                        Math.sqrt(Math.pow(other.getX() - x, 2) + Math.pow(other.getY() - y, 2));
                return distance < 200; // Minimum 200px apart (was 100)
            });

            if (!wallCollision && !tankCollision) {
                return new double[] {x, y};
            }

            attempts++;
        }

        // Final fallback to center
        return new double[] {game.getMapWidth() / 2.0, game.getMapHeight() / 2.0};
    }

    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public Map<String, GameState> getActiveGames() {
        return new HashMap<>(activeGames);
    }

    /**
     * Record match results and award XP/coins to all players
     */
    private void recordMatchResults(GameState game) {
        String gameId = game.getGameId();
        Long startTime = gameStartTimes.get(gameId);

        if (startTime == null) {
            log.warn("No start time found for game {}, skipping match recording", gameId);
            return;
        }

        int matchDurationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);

        // Calculate placements (sort by kills descending, alive tanks first)
        List<Tank> sortedTanks = game.getTanks().values().stream()
                .sorted((t1, t2) -> {
                    // Alive tanks come first
                    if (t1.isAlive() != t2.isAlive()) {
                        return t1.isAlive() ? -1 : 1;
                    }
                    // Then by kills
                    return Integer.compare(t2.getKills(), t1.getKills());
                })
                .toList();

        // Record each player's match result
        for (int i = 0; i < sortedTanks.size(); i++) {
            Tank tank = sortedTanks.get(i);
            int placement = i + 1; // 1st, 2nd, 3rd, 4th

            // Create match result
            // Use playerName as userId for persistence across games
            MatchResult matchResult = new MatchResult(
                    gameId,
                    tank.getPlayerName(), // Username as persistent ID
                    tank.getPlayerName(),
                    placement,
                    tank.getKills());
            matchResult.setDamageDealt(tank.getDamageDealt());
            matchResult.setMatchDurationSeconds(matchDurationSeconds);
            matchResult.calculateRewards();

            // Record with progression service
            try {
                PlayerProgression updatedProgression = progressionService.recordMatch(matchResult);
                log.info(
                        "Recorded match for {}: Level {}, +{} XP, +{} coins",
                        tank.getPlayerName(),
                        updatedProgression.getLevel(),
                        matchResult.getXpEarned(),
                        matchResult.getCoinsEarned());

                // Broadcast progression update to the player
                messagingTemplate.convertAndSend(
                        "/topic/tankgame/progression/" + tank.getId(),
                        Map.of(
                                "progression", updatedProgression,
                                "matchResult", matchResult));
            } catch (Exception e) {
                log.error(
                        "Failed to record match for {}: {}",
                        tank.getPlayerName(),
                        e.getMessage(),
                        e);
            }
        }

        // Clean up
        gameStartTimes.remove(gameId);
        log.info("Finished recording match results for game {}", gameId);
    }
}
