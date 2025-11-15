package biz.thonbecker.personal.foosball.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Value;

public interface PlayerStats {

    @Value("#{target.id}")
    Long getId();

    @Value("#{target.name}")
    String getName();

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
}
