package solutions.thonbecker.personal.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_option", schema = "trivia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;
}