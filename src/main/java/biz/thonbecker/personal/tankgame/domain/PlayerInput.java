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
}
