package biz.thonbecker.personal.foosball.domain;

import java.time.LocalDateTime;
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
    private LocalDateTime playedAt;

    /**
     * Creates a new game with the specified teams.
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
        this.result = null;
    }

    /**
     * Creates a game with teams and a winner.
     *
     * @param whiteTeam White team
     * @param blackTeam Black team
     * @param result The game result
     */
    public Game(Team whiteTeam, Team blackTeam, GameResult result) {
        this(whiteTeam, blackTeam);
        this.result = result;
    }

    /**
     * Returns the winning team name or "Draw" if no winner.
     *
     * @return Winner description
     */
    public String getWinner() {
        return switch (result) {
            case WHITE_TEAM_WIN -> "White Team";
            case BLACK_TEAM_WIN -> "Black Team";
            default -> "Draw";
        };
    }
}
