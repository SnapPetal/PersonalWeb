package biz.thonbecker.personal.tankgame.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchHistoryRepository extends JpaRepository<MatchHistoryEntity, String> {

    List<MatchHistoryEntity> findByUserIdOrderByPlayedAtDesc(String userId);

    Page<MatchHistoryEntity> findByUserIdOrderByPlayedAtDesc(String userId, Pageable pageable);

    List<MatchHistoryEntity> findByGameId(String gameId);

    long countByUserId(String userId);
}
