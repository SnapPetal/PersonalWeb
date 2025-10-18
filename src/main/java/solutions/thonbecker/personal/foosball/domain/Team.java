package solutions.thonbecker.personal.foosball.domain;

import lombok.Value;

/**
 * Represents a team in a Foosball game.
 * Immutable value object containing two players.
 */
@Value
public class Team {
    String player1;
    String player2;

    /**
     * Creates a team with two players.
     *
     * @param player1 First player name
     * @param player2 Second player name
     * @throws IllegalArgumentException if either player name is null or empty
     */
    public Team(String player1, String player2) {
        if (player1 == null || player1.trim().isEmpty()) {
            throw new IllegalArgumentException("Player 1 name cannot be null or empty");
        }
        if (player2 == null || player2.trim().isEmpty()) {
            throw new IllegalArgumentException("Player 2 name cannot be null or empty");
        }
        this.player1 = player1;
        this.player2 = player2;
    }
}
