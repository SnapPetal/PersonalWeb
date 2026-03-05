package biz.thonbecker.personal.landscape.platform.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandscapePlanRepository extends JpaRepository<LandscapePlanEntity, Long> {

    /**
     * Finds all landscape plans for a specific user, ordered by creation date descending.
     *
     * @param userId User identifier
     * @return List of user's landscape plans
     */
    List<LandscapePlanEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
