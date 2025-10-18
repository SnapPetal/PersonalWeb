package solutions.thonbecker.personal.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question", schema = "trivia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "correct_option_index", nullable = false)
    private Integer correctOptionIndex;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    private List<QuestionOptionEntity> options = new ArrayList<>();
}
