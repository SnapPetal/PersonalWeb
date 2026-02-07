package biz.thonbecker.personal.trivia.platform;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for QuizResult persistence.
 * Package-private to enforce module boundaries.
 */
interface QuizResultRepository extends JpaRepository<QuizResultEntity, Long> {

    List<QuizResultEntity> findByQuizIdOrderByScoreDesc(Long quizId);

    List<QuizResultEntity> findByIsWinnerTrueOrderByCompletedAtDesc();

    @Query("SELECT qr FROM QuizResultEntity qr WHERE qr.isWinner = true ORDER BY qr.score DESC, qr.completedAt DESC")
    List<QuizResultEntity> findTopWinners();

    List<QuizResultEntity> findByPlayerIdOrderByCompletedAtDesc(String playerId);
}
