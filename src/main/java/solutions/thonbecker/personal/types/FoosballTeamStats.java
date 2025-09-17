package solutions.thonbecker.personal.types;

import lombok.Data;

@Data
public class FoosballTeamStats {
    private int wins;
    private double winPercentage;
    private int losses;
    private String formattedWinPercentage;
    private double averageTeamScore;
    private String performanceRating;
    private int player1Id;
    private String player1Name;
    private int player2Id;
    private String player2Name;
    private int gamesPlayedTogether;
    private String teamName;
}
