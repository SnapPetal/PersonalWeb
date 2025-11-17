package biz.thonbecker.personal.tankgame.domain;

import lombok.Data;

@Data
public class Projectile {
    private String id;
    private String ownerId; // Tank ID that shot this
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private double radius = 5;
    private int damage = 25;
    private boolean active = true;

    public Projectile(String id, String ownerId, double x, double y, double angle) {
        this.id = id;
        this.ownerId = ownerId;
        this.x = x;
        this.y = y;

        double speed = 400; // pixels per second
        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
    }

    public void update(double deltaTime) {
        if (!active) return;

        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
    }

    public boolean collidesWith(Tank tank) {
        if (!active || !tank.isAlive()) return false;

        // Circle-rectangle collision
        double closestX = Math.max(tank.getX(), Math.min(x, tank.getX() + tank.getWidth()));
        double closestY = Math.max(tank.getY(), Math.min(y, tank.getY() + tank.getHeight()));

        double distanceX = x - closestX;
        double distanceY = y - closestY;

        return (distanceX * distanceX + distanceY * distanceY) < (radius * radius);
    }

    public boolean collidesWith(Wall wall) {
        if (!active) return false;

        // Circle-rectangle collision
        double closestX = Math.max(wall.getX(), Math.min(x, wall.getX() + wall.getWidth()));
        double closestY = Math.max(wall.getY(), Math.min(y, wall.getY() + wall.getHeight()));

        double distanceX = x - closestX;
        double distanceY = y - closestY;

        return (distanceX * distanceX + distanceY * distanceY) < (radius * radius);
    }

    public void deactivate() {
        this.active = false;
    }
}
