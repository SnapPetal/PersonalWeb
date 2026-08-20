package biz.thonbecker.personal.tankgame.domain;

import lombok.Data;

@Data
public class PlayerInput {
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean shoot;
    private double mouseX;
    private double mouseY;

    public PlayerInput sanitized(final double mapWidth, final double mapHeight) {
        final var safe = new PlayerInput();
        safe.setUp(up);
        safe.setDown(down);
        safe.setLeft(left);
        safe.setRight(right);
        safe.setShoot(shoot);
        safe.setMouseX(Double.isFinite(mouseX) ? Math.clamp(mouseX, 0, mapWidth) : mapWidth / 2);
        safe.setMouseY(Double.isFinite(mouseY) ? Math.clamp(mouseY, 0, mapHeight) : mapHeight / 2);
        return safe;
    }
}
