package biz.thonbecker.personal.trivia.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private Long id;
    private String questionText;
    private List<String> options;
    private int correctAnswerIndex;
}
