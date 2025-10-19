package biz.thonbecker.personal.foosball.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoosballGame {
    private Long id;

    @JsonAlias({"whiteTeamPlayer1", "whiteTeamPlayer1Name"})
    private String whiteTeamPlayer1;

    @JsonAlias({"whiteTeamPlayer2", "whiteTeamPlayer2Name"})
    private String whiteTeamPlayer2;

    @JsonAlias({"blackTeamPlayer1", "blackTeamPlayer1Name"})
    private String blackTeamPlayer1;

    @JsonAlias({"blackTeamPlayer2", "blackTeamPlayer2Name"})
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

    private boolean draw;
    private boolean whiteTeamWinner;
    private boolean blackTeamWinner;

    private String username;

    public String getGameDate() {
        return gameDate != null ? gameDate : playedAt;
    }

    @JsonProperty("winner")
    public void setWinner(String winner) {
        this.winner = winner;
        // Reset flags
        this.whiteTeamWinner = false;
        this.blackTeamWinner = false;
        this.draw = false;
        if (winner == null) {
            return;
        }
        String normalized = winner.trim().toUpperCase();
        switch (normalized) {
            case "WHITE":
            case "WHITE_TEAM":
                this.whiteTeamWinner = true;
                break;
            case "BLACK":
            case "BLACK_TEAM":
                this.blackTeamWinner = true;
                break;
            case "DRAW":
            case "TIE":
                this.draw = true;
                break;
            default:
                // If not a known token, try to infer from scores if available
                inferWinnerFromScores();
        }
    }

    // If winner was not provided in a known enum format, infer via scores
    private void inferWinnerFromScores() {
        if (whiteTeamScore != null && blackTeamScore != null) {
            if (whiteTeamScore > blackTeamScore) {
                this.whiteTeamWinner = true;
            } else if (blackTeamScore > whiteTeamScore) {
                this.blackTeamWinner = true;
            } else {
                this.draw = true;
            }
        }
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

    // Keep legacy human-readable winner computation for any usages relying on it
    public String getComputedWinnerLabel() {
        if (whiteTeamScore != null && blackTeamScore != null) {
            if (whiteTeamScore > blackTeamScore) {
                return "White Team";
            } else if (blackTeamScore > whiteTeamScore) {
                return "Black Team";
            } else {
                return "Tie";
            }
        }
        if (whiteTeamWinner) return "White Team";
        if (blackTeamWinner) return "Black Team";
        if (draw) return "Tie";
        return winner; // fallback
    }
}
