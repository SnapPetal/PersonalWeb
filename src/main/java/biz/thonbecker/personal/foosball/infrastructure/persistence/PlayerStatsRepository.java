package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PlayerStatsRepository extends Repository<Player, Long> {

    @Query(
            value =
                    "SELECT id, name, total_games, wins, win_percentage FROM foosball.player_stats ORDER BY win_percentage DESC, total_games DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByWinPercentage();

    @Query(
            value = "SELECT id, name, total_games, wins, win_percentage, "
                    + "(1000 + ((wins * 25) - ((total_games - wins) * 10)) + (total_games * 2)) AS rank_score "
                    + "FROM foosball.player_stats "
                    + "WHERE total_games >= 5 "
                    + "ORDER BY rank_score DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByRankScore();

    @Query(
            value =
                    "SELECT id, name, total_games, wins, win_percentage FROM foosball.player_stats ORDER BY total_games DESC, win_percentage DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByTotalGames();

    @Query(
            value =
                    "SELECT id, name, total_games, wins, win_percentage FROM foosball.player_stats ORDER BY wins DESC, win_percentage DESC",
            nativeQuery = true)
    List<PlayerStats> findAllPlayerStatsOrderedByWins();

    @Query(
            value =
                    "SELECT id, name, total_games, wins, win_percentage FROM foosball.player_stats WHERE total_games >= :minGames ORDER BY win_percentage DESC",
            nativeQuery = true)
    List<PlayerStats> findTopPlayersByWinPercentage(@Param("minGames") int minGames);

    @Query(
            value =
                    "SELECT id, name, total_games, wins, win_percentage FROM foosball.player_stats WHERE total_games >= :minGames ORDER BY total_games DESC",
            nativeQuery = true)
    List<PlayerStats> findTopPlayersByTotalGames(@Param("minGames") int minGames);

    @Query(
            value =
                    "SELECT id, name, total_games, wins, win_percentage FROM foosball.player_stats WHERE total_games >= :minGames ORDER BY wins DESC",
            nativeQuery = true)
    List<PlayerStats> findTopPlayersByWins(@Param("minGames") int minGames);
}
