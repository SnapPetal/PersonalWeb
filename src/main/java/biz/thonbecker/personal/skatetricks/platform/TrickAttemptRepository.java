package biz.thonbecker.personal.skatetricks.platform;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrickAttemptRepository extends JpaRepository<TrickAttemptEntity, Long> {

    List<TrickAttemptEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
