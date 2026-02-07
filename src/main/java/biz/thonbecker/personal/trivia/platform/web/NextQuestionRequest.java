package biz.thonbecker.personal.trivia.platform.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class NextQuestionRequest {
    private Long quizId;
}
