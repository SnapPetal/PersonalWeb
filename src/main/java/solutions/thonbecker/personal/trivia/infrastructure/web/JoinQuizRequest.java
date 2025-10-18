package solutions.thonbecker.personal.trivia.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import solutions.thonbecker.personal.trivia.domain.Player;

@Data
@NoArgsConstructor
@AllArgsConstructor
class JoinQuizRequest {
    private Long quizId;
    private Player player;
}
