package biz.thonbecker.personal.foosball.domain;

import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * Immutable value object representing a player's statistics.
 */
@Value
public class PlayerStats {
    String playerName;
    int rating;

    @Nullable
    Integer peakRating;

    @Nullable
    Integer currentStreak;

    @Nullable
    Integer bestStreak;

    int gamesPlayed;
    int totalGames;
    int wins;
    int losses;
    int draws;
    int goalsScored;
    int goalsAgainst;
    double winPercentage;
    RankTier rankTier;

    /**
     * Creates player statistics.
     *
     * @param playerName Player's name
     * @param rating Current ELO rating
     * @param peakRating Peak ELO rating achieved
     * @param currentStreak Current win/loss streak
     * @param bestStreak Best win streak
     * @param gamesPlayed Total games played (from rating system)
     * @param totalGames Total games played
     * @param wins Number of wins
     * @param losses Number of losses
     * @param draws Number of draws
     * @param goalsScored Total goals scored
     * @param goalsAgainst Total goals against
     */
    public PlayerStats(
            String playerName,
            int rating,
            @Nullable Integer peakRating,
            @Nullable Integer currentStreak,
            @Nullable Integer bestStreak,
            int gamesPlayed,
            int totalGames,
            int wins,
            int losses,
            int draws,
            int goalsScored,
            int goalsAgainst) {
        this.playerName = playerName;
        this.rating = rating;
        this.peakRating = peakRating;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.gamesPlayed = gamesPlayed;
        this.totalGames = totalGames;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.goalsScored = goalsScored;
        this.goalsAgainst = goalsAgainst;
        this.winPercentage = totalGames > 0 ? (double) wins / totalGames * 100 : 0.0;
        this.rankTier = RankTier.fromRating(rating);
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
