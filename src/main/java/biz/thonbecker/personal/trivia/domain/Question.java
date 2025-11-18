package biz.thonbecker.personal.trivia.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private Long id;
    private String questionText;
    private List<String> options;
    private int correctAnswerIndex;
}
