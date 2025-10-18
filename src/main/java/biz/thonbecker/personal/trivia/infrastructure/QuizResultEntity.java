package biz.thonbecker.personal.trivia.infrastructure;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_results", schema = "trivia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String quizTitle;

    @Column(nullable = false)
    private Long quizId;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String playerId;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Integer correctAnswers;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Boolean isWinner;

    @Column
    private String difficulty;
}
