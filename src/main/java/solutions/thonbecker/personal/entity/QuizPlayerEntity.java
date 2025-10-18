package solutions.thonbecker.personal.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "quiz_player", schema = "trivia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(QuizPlayerEntity.QuizPlayerKey.class)
public class QuizPlayerEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "is_ready", nullable = false)
    private Boolean isReady = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizPlayerKey implements Serializable {
        private Long quiz;
        private String player;
    }
}