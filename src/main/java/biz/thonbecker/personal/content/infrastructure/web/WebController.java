package biz.thonbecker.personal.content.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/trivia")
    public String trivia() {
        return "trivia";
    }
}
