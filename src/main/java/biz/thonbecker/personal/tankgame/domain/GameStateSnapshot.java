package biz.thonbecker.personal.tankgame.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable wire representation used by real-time clients. */
public record GameStateSnapshot(
        String gameId,
        GameState.GameStatus status,
        int mapWidth,
        int mapHeight,
        String winnerId,
        String winnerName,
        Map<String, TankSnapshot> tanks,
        List<ProjectileSnapshot> projectiles,
        List<WallSnapshot> walls) {
    public static GameStateSnapshot from(final GameState game) {
        final var tanks = new LinkedHashMap<String, TankSnapshot>();
        game.getTanks().forEach((id, tank) -> tanks.put(id, TankSnapshot.from(tank)));
        return new GameStateSnapshot(
                game.getGameId(),
                game.getStatus(),
                game.getMapWidth(),
                game.getMapHeight(),
                game.getWinnerId(),
                game.getWinnerName(),
                tanks,
                game.getProjectiles().stream().map(ProjectileSnapshot::from).toList(),
                game.getWalls().stream().map(WallSnapshot::from).toList());
    }

    public record TankSnapshot(
            String id,
            String playerName,
            double x,
            double y,
            double width,
            double height,
            int health,
            int maxHealth,
            String color,
            String loadoutId,
            boolean bot,
            double rotation,
            boolean alive,
            int kills) {
        static TankSnapshot from(final Tank tank) {
            return new TankSnapshot(
                    tank.getId(),
                    tank.getPlayerName(),
                    tank.getX(),
                    tank.getY(),
                    tank.getWidth(),
                    tank.getHeight(),
                    tank.getHealth(),
                    tank.getMaxHealth(),
                    tank.getColor(),
                    tank.getLoadoutId(),
                    tank.isBot(),
                    tank.getRotation(),
                    tank.isAlive(),
                    tank.getKills());
        }
    }

    public record ProjectileSnapshot(String id, double x, double y, double radius) {
        static ProjectileSnapshot from(final Projectile projectile) {
            return new ProjectileSnapshot(
                    projectile.getId(), projectile.getX(), projectile.getY(), projectile.getRadius());
        }
    }

    public record WallSnapshot(double x, double y, double width, double height) {
        static WallSnapshot from(final Wall wall) {
            return new WallSnapshot(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
        }
    }
}
