package solutions.thonbecker.personal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solutions.thonbecker.personal.entity.AnswerSubmissionEntity;

import java.util.List;

@Repository
public interface AnswerSubmissionRepository extends JpaRepository<AnswerSubmissionEntity, Long> {
    List<AnswerSubmissionEntity> findByQuizIdOrderBySubmissionTimeAsc(Long quizId);

    List<AnswerSubmissionEntity> findByPlayerIdAndQuizId(String playerId, Long quizId);
}