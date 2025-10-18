package solutions.thonbecker.personal.types.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextQuestionRequest {
    private Long quizId;
}