package solutions.thonbecker.personal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solutions.thonbecker.personal.entity.QuizEntity;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Long> {}