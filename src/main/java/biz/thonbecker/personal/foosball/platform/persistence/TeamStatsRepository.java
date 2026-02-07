package biz.thonbecker.personal.foosball.platform.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TeamStatsRepository extends Repository<Player, Long> {

    @Query(
            value =
                    "SELECT player1_id, player1_name, player2_id, player2_name, games_played_together, wins, win_percentage, avg_team_score FROM foosball.team_stats ORDER BY win_percentage DESC",
            nativeQuery = true)
    List<TeamStats> findAllTeamStatsOrderedByWinPercentage();

    @Query(
            value =
                    "SELECT player1_id, player1_name, player2_id, player2_name, games_played_together, wins, win_percentage, avg_team_score FROM foosball.team_stats ORDER BY games_played_together DESC",
            nativeQuery = true)
    List<TeamStats> findAllTeamStatsOrderedByGamesPlayed();

    @Query(
            value =
                    "SELECT player1_id, player1_name, player2_id, player2_name, games_played_together, wins, win_percentage, avg_team_score FROM foosball.team_stats WHERE games_played_together >= :minGames ORDER BY win_percentage DESC",
            nativeQuery = true)
    List<TeamStats> findTopTeamsByWinPercentage(@Param("minGames") int minGames);

    @Query(
            value =
                    "SELECT player1_id, player1_name, player2_id, player2_name, games_played_together, wins, win_percentage, avg_team_score FROM foosball.team_stats WHERE games_played_together >= :minGames ORDER BY avg_team_score DESC",
            nativeQuery = true)
    List<TeamStats> findTopTeamsByAverageScore(@Param("minGames") int minGames);
}
