package biz.thonbecker.personal.tankgame.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

@Data
public class GameState {
    private String gameId;
    private Map<String, Tank> tanks = new ConcurrentHashMap<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Wall> walls = new ArrayList<>();
    private GameStatus status = GameStatus.WAITING;
    private int mapWidth = 800;
    private int mapHeight = 600;
    private long createdAt;
    private String winnerId;
    private String winnerName;

    public GameState() {
        this.gameId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        initializeWalls();
    }

    private void initializeWalls() {
        // Border walls
        walls.add(new Wall(0, 0, mapWidth, 10)); // Top
        walls.add(new Wall(0, mapHeight - 10, mapWidth, 10)); // Bottom
        walls.add(new Wall(0, 0, 10, mapHeight)); // Left
        walls.add(new Wall(mapWidth - 10, 0, 10, mapHeight)); // Right

        // Interior obstacles
        walls.add(new Wall(200, 150, 100, 20));
        walls.add(new Wall(500, 150, 100, 20));
        walls.add(new Wall(200, 430, 100, 20));
        walls.add(new Wall(500, 430, 100, 20));
        walls.add(new Wall(350, 250, 100, 100));
    }

    public void addTank(Tank tank) {
        tanks.put(tank.getId(), tank);
        if (tanks.size() >= 2 && status == GameStatus.WAITING) {
            status = GameStatus.PLAYING;
        }
    }

    public void removeTank(String tankId) {
        tanks.remove(tankId);
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    public int getAliveTankCount() {
        return (int) tanks.values().stream().filter(Tank::isAlive).count();
    }

    public Tank getWinningTank() {
        return tanks.values().stream().filter(Tank::isAlive).findFirst().orElse(null);
    }

    public void checkGameOver() {
        if (status != GameStatus.PLAYING) return;

        int aliveCount = getAliveTankCount();
        if (aliveCount <= 1) {
            status = GameStatus.FINISHED;
            Tank winner = getWinningTank();
            if (winner != null) {
                winnerId = winner.getId();
                winnerName = winner.getPlayerName();
            }
        }
    }

    public enum GameStatus {
        WAITING,
        PLAYING,
        FINISHED
    }
}
