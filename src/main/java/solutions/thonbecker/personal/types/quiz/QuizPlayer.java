package solutions.thonbecker.personal.types.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizPlayer {
    private String id;
    private String name;
    private int score;

    public QuizPlayer(String id, String name) {
        this.id = id;
        this.name = name;
        this.score = 0;
    }
}
