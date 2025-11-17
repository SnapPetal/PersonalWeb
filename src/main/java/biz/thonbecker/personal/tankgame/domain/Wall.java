package biz.thonbecker.personal.tankgame.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Wall {
    private double x;
    private double y;
    private double width;
    private double height;
}
