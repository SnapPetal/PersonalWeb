package solutions.thonbecker.personal.foosball.domain;

import lombok.Value;

/**
 * Immutable value object representing team statistics.
 */
@Value
public class TeamStats {
    String player1;
    String player2;
    int gamesPlayed;
    int wins;
    int losses;
    int draws;
    int goalsScored;
    int goalsAgainst;
    double winPercentage;

    /**
     * Creates team statistics.
     *
     * @param player1 First player name
     * @param player2 Second player name
     * @param gamesPlayed Total games played together
     * @param wins Number of wins
     * @param losses Number of losses
     * @param draws Number of draws
     * @param goalsScored Total goals scored
     * @param goalsAgainst Total goals against
     */
    public TeamStats(
            String player1,
            String player2,
            int gamesPlayed,
            int wins,
            int losses,
            int draws,
            int goalsScored,
            int goalsAgainst) {
        this.player1 = player1;
        this.player2 = player2;
        this.gamesPlayed = gamesPlayed;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.goalsScored = goalsScored;
        this.goalsAgainst = goalsAgainst;
        this.winPercentage = gamesPlayed > 0 ? (double) wins / gamesPlayed * 100 : 0.0;
    }

    /**
     * Calculates the goal difference.
     *
     * @return Goals scored minus goals against
     */
    public int getGoalDifference() {
        return goalsScored - goalsAgainst;
    }

    /**
     * Gets the team name as "player1 & player2".
     *
     * @return Team name
     */
    public String getTeamName() {
        return player1 + " & " + player2;
    }
}
