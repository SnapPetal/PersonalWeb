package biz.thonbecker.personal.trivia.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class StartQuizRequest {
    private Long quizId;
}
