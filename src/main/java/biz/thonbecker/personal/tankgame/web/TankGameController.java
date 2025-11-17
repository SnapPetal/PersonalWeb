package biz.thonbecker.personal.tankgame.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TankGameController {

    @GetMapping("/tankgame")
    public String tankGame() {
        return "tankgame";
    }
}
