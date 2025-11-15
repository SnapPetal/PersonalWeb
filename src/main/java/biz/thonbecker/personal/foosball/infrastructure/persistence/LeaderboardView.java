package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.math.BigDecimal;

/**
 * Projection for tournament standings/leaderboard
 */
public interface LeaderboardView {
    Long getRegistrationId();

    String getDisplayName();

    Integer getPosition();

    BigDecimal getPoints();

    Integer getWins();

    Integer getLosses();

    Integer getDraws();

    Integer getGamesPlayed();

    Integer getGoalsFor();

    Integer getGoalsAgainst();

    Integer getGoalDifference();

    default Double getWinPercentage() {
        return getGamesPlayed() == 0 ? 0.0 : (double) getWins() / getGamesPlayed() * 100;
    }

    default Double getPointsPerGame() {
        return getGamesPlayed() == 0 ? 0.0 : getPoints().doubleValue() / getGamesPlayed();
    }
}
