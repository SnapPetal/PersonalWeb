package biz.thonbecker.personal.landscape.platform.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LandscapePlanRepository extends JpaRepository<LandscapePlanEntity, Long> {

    /**
     * Finds all landscape plans for a specific user, ordered by creation date descending.
     *
     * @param userId User identifier
     * @return List of user's landscape plans
     */
    List<LandscapePlanEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Fetches a plan with placements and recommendations eagerly loaded to avoid N+1 queries.
     *
     * @param planId Plan identifier
     * @return The plan with all associations loaded
     */
    @Query("SELECT p FROM LandscapePlanEntity p LEFT JOIN FETCH p.placements WHERE p.id = :planId")
    Optional<LandscapePlanEntity> findByIdWithPlacements(Long planId);
}
