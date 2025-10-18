package solutions.thonbecker.personal.foosball.domain;

import lombok.Value;

/**
 * Immutable value object representing a player's statistics.
 */
@Value
public class PlayerStats {
    String playerName;
    int totalGames;
    int wins;
    int losses;
    int draws;
    int goalsScored;
    int goalsAgainst;
    double winPercentage;

    /**
     * Creates player statistics.
     *
     * @param playerName Player's name
     * @param totalGames Total games played
     * @param wins Number of wins
     * @param losses Number of losses
     * @param draws Number of draws
     * @param goalsScored Total goals scored
     * @param goalsAgainst Total goals against
     */
    public PlayerStats(
            String playerName,
            int totalGames,
            int wins,
            int losses,
            int draws,
            int goalsScored,
            int goalsAgainst) {
        this.playerName = playerName;
        this.totalGames = totalGames;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.goalsScored = goalsScored;
        this.goalsAgainst = goalsAgainst;
        this.winPercentage = totalGames > 0 ? (double) wins / totalGames * 100 : 0.0;
    }

    /**
     * Calculates the goal difference.
     *
     * @return Goals scored minus goals against
     */
    public int getGoalDifference() {
        return goalsScored - goalsAgainst;
    }
}
