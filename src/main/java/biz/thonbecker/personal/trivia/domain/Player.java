package biz.thonbecker.personal.trivia.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Player {
    private String id;
    private String name;
    private int score = 0;
}
