package biz.thonbecker.personal.tankgame.domain;

import lombok.Data;

@Data
public class Tank {
    private String id;
    private String playerName;
    private double x;
    private double y;
    private double width = 40;
    private double height = 40;
    private int health = 100;
    private int maxHealth = 100;
    private String color;
    private double rotation = 0; // radians
    private boolean alive = true;
    private int kills = 0;
    private int damageDealt = 0;
    private long lastShotTime = 0;
    private static final long SHOT_COOLDOWN_MS = 500; // 0.5 seconds between shots

    public Tank(String id, String playerName, double x, double y, String color) {
        this.id = id;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void move(PlayerInput input, double deltaTime) {
        if (!alive) return;

        double speed = 150; // pixels per second
        double moveDistance = speed * deltaTime;

        double newX = x;
        double newY = y;

        if (input.isUp()) newY -= moveDistance;
        if (input.isDown()) newY += moveDistance;
        if (input.isLeft()) newX -= moveDistance;
        if (input.isRight()) newX += moveDistance;

        // Store potential new position (collision will be checked by service)
        this.x = newX;
        this.y = newY;
    }

    public void takeDamage(int damage, Tank attacker) {
        if (!alive) return;
        health -= damage;
        if (attacker != null) {
            attacker.addDamageDealt(damage);
        }
        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public void addDamageDealt(int damage) {
        this.damageDealt += damage;
    }

    public boolean canShoot() {
        return alive && System.currentTimeMillis() - lastShotTime >= SHOT_COOLDOWN_MS;
    }

    public void recordShot() {
        lastShotTime = System.currentTimeMillis();
    }

    public boolean collidesWith(Tank other) {
        return this.x < other.x + other.width
                && this.x + this.width > other.x
                && this.y < other.y + other.height
                && this.y + this.height > other.y;
    }

    public boolean collidesWith(Wall wall) {
        return this.x < wall.getX() + wall.getWidth()
                && this.x + this.width > wall.getX()
                && this.y < wall.getY() + wall.getHeight()
                && this.y + this.height > wall.getY();
    }

    public void addKill() {
        kills++;
    }
}
