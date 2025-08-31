package solutions.thonbecker.personal.types;

import lombok.Data;

@Data
public class FoosballStats {
    private String playerName;
    private Integer gamesPlayed;
    private Integer wins;
    private Double winPercentage;
    private Integer goalsScored;
    private Integer goalsConceded;
    private String position;
}
