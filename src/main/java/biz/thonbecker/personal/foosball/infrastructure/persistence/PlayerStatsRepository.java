package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PlayerStatsRepository extends Repository<Player, Long> {

    @Query(
            value =
                    "SELECT id, name, rating, peak_rating, current_streak, best_streak, games_played, total_games, wins, win_percentage FROM foosball.player_stats ORDER BY rating DESC, win_percentage DESC, total_games DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByWinPercentage();

    // Removed: Old rank_score calculation replaced with ELO rating system
    // Use Player.rating field instead via PlayerRepository

    @Query(
            value =
                    "SELECT id, name, rating, peak_rating, current_streak, best_streak, games_played, total_games, wins, win_percentage FROM foosball.player_stats ORDER BY total_games DESC, win_percentage DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByTotalGames();

    @Query(
            value =
                    "SELECT id, name, rating, peak_rating, current_streak, best_streak, games_played, total_games, wins, win_percentage FROM foosball.player_stats ORDER BY wins DESC, win_percentage DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByWins();

    @Query(
            value =
                    "SELECT id, name, rating, peak_rating, current_streak, best_streak, games_played, total_games, wins, win_percentage FROM foosball.player_stats WHERE total_games >= :minGames ORDER BY rating DESC, win_percentage DESC",
            nativeQuery = true)
    List<PlayerStats> findTopPlayersByWinPercentage(@Param("minGames") int minGames);

    @Query(
            value =
                    "SELECT id, name, rating, peak_rating, current_streak, best_streak, games_played, total_games, wins, win_percentage FROM foosball.player_stats WHERE total_games >= :minGames ORDER BY total_games DESC",
            nativeQuery = true)
    List<PlayerStats> findTopPlayersByTotalGames(@Param("minGames") int minGames);

    @Query(
            value =
                    "SELECT id, name, rating, peak_rating, current_streak, best_streak, games_played, total_games, wins, win_percentage FROM foosball.player_stats WHERE total_games >= :minGames ORDER BY wins DESC",
            nativeQuery = true)
    List<PlayerStats> findTopPlayersByWins(@Param("minGames") int minGames);
}
