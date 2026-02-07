package biz.thonbecker.personal.foosball.platform.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {

    /**
     * Find all rating history for a player, ordered by most recent first
     */
    List<RatingHistory> findByPlayerOrderByRecordedAtDesc(Player player);

    /**
     * Find top N rating history entries for a player
     */
    @Query(value = """
        SELECT rh FROM RatingHistory rh
        WHERE rh.player = :player
        ORDER BY rh.recordedAt DESC
        LIMIT :limit
        """)
    List<RatingHistory> findTopNByPlayerOrderByRecordedAtDesc(
            @Param("player") Player player, @Param("limit") int limit);
}
