package biz.thonbecker.personal.tankgame.application;

import biz.thonbecker.personal.tankgame.domain.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TankGameService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ProgressionService progressionService;
    private final TankGameRoomService roomService;
    private long lastUpdateTime = System.currentTimeMillis();

    public TankGameService(
            SimpMessagingTemplate messagingTemplate,
            ProgressionService progressionService,
            TankGameRoomService roomService) {
        this.messagingTemplate = messagingTemplate;
        this.progressionService = progressionService;
        this.roomService = roomService;
    }

    public GameState createGame() {
        return roomService.createGame();
    }

    public synchronized GameState findOrCreateWaitingGame() {
        return roomService.findOrCreateWaitingGame();
    }

    public Tank joinGame(String gameId, String playerName) {
        final var tank = roomService.joinGame(gameId, playerName);
        final var game = roomService.getGame(gameId);
        if (game != null) broadcastGameState(game);
        return tank;
    }

    public void leaveGame(String gameId, String tankId) {
        roomService.leaveGame(gameId, tankId);
        final var game = roomService.getGame(gameId);
        if (game != null) broadcastGameState(game);
    }

    public void updateInput(String tankId, PlayerInput input) {
        roomService.updateInput(tankId, input);
    }

    @Scheduled(fixedRate = 16) // ~60 FPS
    public void gameLoop() {
        long now = System.currentTimeMillis();
        double rawDeltaTime = (now - lastUpdateTime) / 1000.0;
        lastUpdateTime = now;

        // Limit deltaTime to prevent huge jumps
        final double deltaTime = Math.min(rawDeltaTime, 0.1);

        roomService.addAiOpponents(now);
        roomService.getActiveGames().values().forEach(game -> {
            if (game.getStatus() != GameState.GameStatus.PLAYING) {
                return;
            }

            updateGame(game, deltaTime);
            broadcastGameState(game);
        });
    }

    private void updateGame(GameState game, double deltaTime) {
        game.getTanks().values().stream()
                .filter(Tank::isBot)
                .filter(Tank::isAlive)
                .forEach(tank -> updateBotInput(game, tank));

        // Update tanks based on input
        game.getTanks().values().forEach(tank -> {
            if (!tank.isAlive()) return;

            PlayerInput input = roomService.getInput(tank.getId());
            if (input == null) return;

            // Store old position for collision rollback
            double oldX = tank.getX();
            double oldY = tank.getY();

            // Update rotation based on mouse position
            double dx = input.getMouseX() - (tank.getX() + tank.getWidth() / 2);
            double dy = input.getMouseY() - (tank.getY() + tank.getHeight() / 2);
            tank.setRotation(Math.atan2(dy, dx));

            // Move tank
            tank.move(input, deltaTime);

            // Check collisions with walls
            boolean wallCollision = game.getWalls().stream().anyMatch(wall -> tank.collidesWith(wall));

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
            boolean hitWall = game.getWalls().stream().anyMatch(wall -> projectile.collidesWith(wall));

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
        if (previousStatus == GameState.GameStatus.PLAYING && game.getStatus() == GameState.GameStatus.FINISHED) {
            recordMatchResults(game);
        }
    }

    private void broadcastGameState(GameState game) {
        messagingTemplate.convertAndSend("/topic/tankgame/" + game.getGameId(), game);
    }

    public GameState getGame(String gameId) {
        return roomService.getGame(gameId);
    }

    public Map<String, GameState> getActiveGames() {
        return roomService.getActiveGames();
    }

    /**
     * Record match results and award XP/coins to all players
     */
    private void recordMatchResults(GameState game) {
        String gameId = game.getGameId();
        Long startTime = roomService.getGameStartTime(gameId);

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
            if (tank.isBot()) {
                continue;
            }
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
                Object payload = Map.of(
                        "progression", updatedProgression,
                        "matchResult", matchResult);
                messagingTemplate.convertAndSend("/topic/tankgame/progression/" + tank.getId(), payload);
            } catch (Exception e) {
                log.error("Failed to record match for {}: {}", tank.getPlayerName(), e.getMessage(), e);
            }
        }

        // Clean up
        roomService.removeGameStartTime(gameId);
        log.info("Finished recording match results for game {}", gameId);
    }

    private void updateBotInput(final GameState game, final Tank bot) {
        final var target = game.getTanks().values().stream()
                .filter(tank -> !tank.isBot())
                .filter(Tank::isAlive)
                .findFirst()
                .orElse(null);
        if (target == null) {
            return;
        }

        final var input = roomService.getOrCreateInput(bot.getId());
        final double targetX = target.getX() + target.getWidth() / 2;
        final double targetY = target.getY() + target.getHeight() / 2;
        final double botX = bot.getX() + bot.getWidth() / 2;
        final double botY = bot.getY() + bot.getHeight() / 2;
        input.setUp(targetY < botY - 20);
        input.setDown(targetY > botY + 20);
        input.setLeft(targetX < botX - 20);
        input.setRight(targetX > botX + 20);
        input.setMouseX(targetX);
        input.setMouseY(targetY);
        input.setShoot(true);
    }
}
