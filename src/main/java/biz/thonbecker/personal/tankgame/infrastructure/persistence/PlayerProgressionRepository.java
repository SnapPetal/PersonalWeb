package biz.thonbecker.personal.tankgame.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerProgressionRepository extends JpaRepository<PlayerProgressionEntity, String> {

    Optional<PlayerProgressionEntity> findByUserId(String userId);

    Optional<PlayerProgressionEntity> findByUsername(String username);

    // Leaderboard queries
    List<PlayerProgressionEntity> findTop10ByOrderByTotalXpDesc();

    List<PlayerProgressionEntity> findTop10ByOrderByLevelDescTotalXpDesc();

    List<PlayerProgressionEntity> findTop10ByOrderByTotalKillsDesc();

    @Query(
            "SELECT p FROM PlayerProgressionEntity p WHERE p.totalGames > 0 ORDER BY (CAST(p.totalWins AS double) / p.totalGames) DESC")
    List<PlayerProgressionEntity> findTop10ByWinRate();
}
