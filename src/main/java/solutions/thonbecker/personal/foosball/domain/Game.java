package solutions.thonbecker.personal.foosball.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Core domain entity representing a Foosball game.
 * Contains business logic for game state and winner determination.
 */
@Data
@NoArgsConstructor
public class Game {
    private Long id;
    private Team whiteTeam;
    private Team blackTeam;
    private int whiteTeamScore;
    private int blackTeamScore;
    private GameResult result;

    /**
     * Creates a new game with the specified teams.
     * Scores default to 0 and result is calculated after scores are set.
     *
     * @param whiteTeam The white team
     * @param blackTeam The black team
     */
    public Game(Team whiteTeam, Team blackTeam) {
        if (whiteTeam == null) {
            throw new IllegalArgumentException("White team cannot be null");
        }
        if (blackTeam == null) {
            throw new IllegalArgumentException("Black team cannot be null");
        }
        this.whiteTeam = whiteTeam;
        this.blackTeam = blackTeam;
        this.whiteTeamScore = 0;
        this.blackTeamScore = 0;
        this.result = determineResult();
    }

    /**
     * Creates a game with teams and final scores.
     *
     * @param whiteTeam White team
     * @param blackTeam Black team
     * @param whiteTeamScore White team score
     * @param blackTeamScore Black team score
     */
    public Game(Team whiteTeam, Team blackTeam, int whiteTeamScore, int blackTeamScore) {
        this(whiteTeam, blackTeam);
        setScores(whiteTeamScore, blackTeamScore);
    }

    /**
     * Sets the scores for both teams and recalculates the result.
     *
     * @param whiteScore White team score
     * @param blackScore Black team score
     * @throws IllegalArgumentException if scores are negative
     */
    public void setScores(int whiteScore, int blackScore) {
        if (whiteScore < 0 || blackScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative");
        }
        this.whiteTeamScore = whiteScore;
        this.blackTeamScore = blackScore;
        this.result = determineResult();
    }

    /**
     * Determines the game result based on current scores.
     *
     * @return The game result (WHITE_TEAM_WIN, BLACK_TEAM_WIN, or DRAW)
     */
    private GameResult determineResult() {
        if (whiteTeamScore > blackTeamScore) {
            return GameResult.WHITE_TEAM_WIN;
        } else if (blackTeamScore > whiteTeamScore) {
            return GameResult.BLACK_TEAM_WIN;
        } else {
            return GameResult.DRAW;
        }
    }

    /**
     * Returns the winning team name or "Draw" if no winner.
     *
     * @return Winner description
     */
    public String getWinner() {
        switch (result) {
            case WHITE_TEAM_WIN:
                return "White Team";
            case BLACK_TEAM_WIN:
                return "Black Team";
            case DRAW:
            default:
                return "Draw";
        }
    }

    /**
     * Checks if the white team won.
     *
     * @return true if white team won
     */
    public boolean isWhiteTeamWinner() {
        return result == GameResult.WHITE_TEAM_WIN;
    }

    /**
     * Checks if the black team won.
     *
     * @return true if black team won
     */
    public boolean isBlackTeamWinner() {
        return result == GameResult.BLACK_TEAM_WIN;
    }

    /**
     * Checks if the game ended in a draw.
     *
     * @return true if game is a draw
     */
    public boolean isDraw() {
        return result == GameResult.DRAW;
    }
}
