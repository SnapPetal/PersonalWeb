package biz.thonbecker.personal.trivia.platform.web;

import biz.thonbecker.personal.trivia.domain.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class JoinQuizRequest {
    private Long quizId;
    private Player player;
}
