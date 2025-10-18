package solutions.thonbecker.personal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import solutions.thonbecker.personal.entity.QuizResultEntity;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResultEntity, Long> {

    List<QuizResultEntity> findByQuizIdOrderByScoreDesc(Long quizId);

    List<QuizResultEntity> findByIsWinnerTrueOrderByCompletedAtDesc();

    @Query(
            "SELECT qr FROM QuizResultEntity qr WHERE qr.isWinner = true ORDER BY qr.score DESC, qr.completedAt DESC")
    List<QuizResultEntity> findTopWinners();

    List<QuizResultEntity> findByPlayerIdOrderByCompletedAtDesc(String playerId);
}
