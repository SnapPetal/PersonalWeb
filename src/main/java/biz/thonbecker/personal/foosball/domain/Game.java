package biz.thonbecker.personal.foosball.domain;

import java.time.Instant;
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
    private GameResult result;
    private Instant playedAt;

    /**
     * Creates a new game with the specified teams.
     * Note: A result must be set after creation - draws are not allowed.
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
    }

    /**
     * Creates a game with teams and a winner.
     *
     * @param whiteTeam White team
     * @param blackTeam Black team
     * @param result The game result (cannot be null)
     */
    public Game(Team whiteTeam, Team blackTeam, GameResult result) {
        this(whiteTeam, blackTeam);
        if (result == null) {
            throw new IllegalArgumentException("Game result cannot be null - draws are not allowed");
        }
        this.result = result;
    }

    /**
     * Sets the game result.
     *
     * @param result The game result (cannot be null)
     */
    public void setResult(GameResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Game result cannot be null - draws are not allowed");
        }
        this.result = result;
    }

    /**
     * Returns the winning team name.
     *
     * @return Winner description
     */
    public String getWinner() {
        if (result == null) {
            throw new IllegalStateException("Game result has not been set");
        }
        return switch (result) {
            case WHITE_TEAM_WIN -> "White Team";
            case BLACK_TEAM_WIN -> "Black Team";
        };
    }
}
