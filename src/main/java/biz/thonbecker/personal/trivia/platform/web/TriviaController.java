package biz.thonbecker.personal.trivia.platform.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class TriviaController {

    @GetMapping("/trivia")
    String trivia() {
        return "trivia";
    }
}
