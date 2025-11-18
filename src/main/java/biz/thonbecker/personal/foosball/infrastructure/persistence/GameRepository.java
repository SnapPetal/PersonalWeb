package biz.thonbecker.personal.foosball.infrastructure.persistence;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;

@RepositoryRestResource(path = "games", collectionResourceRel = "games", itemResourceRel = "game")
public interface GameRepository extends CrudRepository<Game, Long> {

    @RestResource(path = "by-player", rel = "by-player")
    @Query(
            "SELECT g FROM Game g WHERE g.whiteTeamPlayer1 = :player OR g.whiteTeamPlayer2 = :player OR g.blackTeamPlayer1 = :player OR g.blackTeamPlayer2 = :player ORDER BY g.playedAt DESC")
    List<Game> findByPlayer(@Param("player") Player player);

    @RestResource(path = "by-winner", rel = "by-winner")
    List<Game> findByWinner(Game.TeamColor winner);

    @RestResource(path = "by-date-range", rel = "by-date-range")
    @Query("SELECT g FROM Game g WHERE g.playedAt BETWEEN :startDate AND :endDate ORDER BY g.playedAt DESC")
    List<Game> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @RestResource(path = "recent", rel = "recent")
    @Query("SELECT g FROM Game g " + "LEFT JOIN FETCH g.whiteTeamPlayer1 "
            + "LEFT JOIN FETCH g.whiteTeamPlayer2 "
            + "LEFT JOIN FETCH g.blackTeamPlayer1 "
            + "LEFT JOIN FETCH g.blackTeamPlayer2 "
            + "ORDER BY g.playedAt DESC LIMIT 10")
    List<GameWithPlayers> findRecentGames();

    @RestResource(path = "by-score", rel = "by-score")
    @Query("SELECT g FROM Game g WHERE g.whiteTeamScore = :score OR g.blackTeamScore = :score ORDER BY g.playedAt DESC")
    List<Game> findByScore(@Param("score") Integer score);

    @RestResource(path = "high-scoring", rel = "high-scoring")
    @Query(
            "SELECT g FROM Game g WHERE g.whiteTeamScore + g.blackTeamScore >= :minTotalScore ORDER BY (g.whiteTeamScore + g.blackTeamScore) DESC")
    List<Game> findHighScoringGames(@Param("minTotalScore") Integer minTotalScore);

    // Statistics queries
    @Query("SELECT COUNT(g) FROM Game g WHERE g.winner IS NOT NULL")
    Long countGamesWithWinner();

    @Query("SELECT COUNT(g) FROM Game g WHERE g.winner IS NULL")
    Long countDraws();

    @Query("SELECT AVG(g.whiteTeamScore + g.blackTeamScore) FROM Game g")
    Double getAverageTotalScore();

    @Query("SELECT MAX(g.whiteTeamScore + g.blackTeamScore) FROM Game g")
    Integer getHighestTotalScore();

    @Query("SELECT MIN(g.whiteTeamScore + g.blackTeamScore) FROM Game g")
    Integer getLowestTotalScore();

    @Override
    @RestResource(exported = false)
    void deleteById(@NonNull Long id);

    @Override
    @RestResource(exported = false)
    void delete(@NonNull Game entity);

    @Override
    @RestResource(exported = false)
    void deleteAll(@NonNull Iterable<? extends Game> entities);

    @Override
    @RestResource(exported = false)
    void deleteAll();

    @Modifying
    @Transactional
    @Query("DELETE FROM Game g WHERE g.playedAt < :ninetyDaysAgo")
    int deleteGamesOlderThan(@Param("ninetyDaysAgo") LocalDateTime ninetyDaysAgo);
}
