package biz.thonbecker.personal.content.platform.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "redirect:https://thonbecker.biz";
    }

    @GetMapping("/trivia")
    public String trivia() {
        return "trivia";
    }

    @GetMapping("/religious-freedom")
    public String religiousFreedom() {
        return "religious-freedom";
    }
}
