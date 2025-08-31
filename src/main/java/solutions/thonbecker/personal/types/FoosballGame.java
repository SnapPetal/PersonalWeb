package solutions.thonbecker.personal.types;

import lombok.Data;
import java.util.List;

@Data
public class FoosballGame {
    private Long id;
    private List<FoosballPlayer> team1Players;
    private List<FoosballPlayer> team2Players;
    private Integer team1Score;
    private Integer team2Score;
    private Integer team1GoalieScore;
    private Integer team2GoalieScore;
    private Integer team1ForwardScore;
    private Integer team2ForwardScore;
    private String gameDate;
    private String notes;
    private String winner;
}
