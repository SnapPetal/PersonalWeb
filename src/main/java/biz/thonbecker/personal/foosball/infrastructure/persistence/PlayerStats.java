package biz.thonbecker.personal.foosball.infrastructure.persistence;

import biz.thonbecker.personal.foosball.domain.RankTier;
import org.springframework.beans.factory.annotation.Value;

public interface PlayerStats {

    @Value("#{target.id}")
    Long getId();

    @Value("#{target.name}")
    String getName();

    @Value("#{target.rating}")
    Integer getRating();

    @Value("#{target.peak_rating}")
    Integer getPeakRating();

    @Value("#{target.current_streak}")
    Integer getCurrentStreak();

    @Value("#{target.best_streak}")
    Integer getBestStreak();

    @Value("#{target.games_played}")
    Integer getGamesPlayed();

    @Value("#{target.total_games}")
    Long getTotalGames();

    @Value("#{target.wins}")
    Long getWins();

    @Value("#{target.win_percentage}")
    Double getWinPercentage();

    default Long getLosses() {
        Long total = getTotalGames();
        Long wins = getWins();
        return total != null && wins != null ? total - wins : 0L;
    }

    default String getFormattedWinPercentage() {
        Double percentage = getWinPercentage();
        if (percentage == null) return "0.0%";
        return String.format("%.1f%%", percentage);
    }

    default RankTier getRankTier() {
        Integer rating = getRating();
        return rating != null ? RankTier.fromRating(rating) : RankTier.BRONZE;
    }
}
