package solutions.thonbecker.personal.types;

import java.util.List;
import lombok.Data;

@Data
public class FoosballGame {
    private Long id;
    private String whiteTeamPlayer1;
    private String whiteTeamPlayer2;
    private String blackTeamPlayer1;
    private String blackTeamPlayer2;
    private Integer whiteTeamScore;
    private Integer blackTeamScore;
    private Integer whiteTeamGoalieScore;
    private Integer blackTeamGoalieScore;
    private Integer whiteTeamForwardScore;
    private Integer blackTeamForwardScore;
    private String gameDate;
    private String playedAt;
    private String notes;
    private String winner;
    
    // Getter for backward compatibility - use playedAt if gameDate is null
    public String getGameDate() {
        return gameDate != null ? gameDate : playedAt;
    }
    
    // Computed properties for backward compatibility
    public List<FoosballPlayer> getTeam1Players() {
        // Create mock player objects for display purposes
        FoosballPlayer player1 = new FoosballPlayer();
        player1.setName(whiteTeamPlayer1);
        FoosballPlayer player2 = new FoosballPlayer();
        player2.setName(whiteTeamPlayer2);
        return List.of(player1, player2);
    }
    
    public List<FoosballPlayer> getTeam2Players() {
        // Create mock player objects for display purposes
        FoosballPlayer player1 = new FoosballPlayer();
        player1.setName(blackTeamPlayer1);
        FoosballPlayer player2 = new FoosballPlayer();
        player2.setName(blackTeamPlayer2);
        return List.of(player1, player2);
    }
    
    public Integer getTeam1Score() {
        return whiteTeamScore;
    }
    
    public Integer getTeam2Score() {
        return blackTeamScore;
    }
}
