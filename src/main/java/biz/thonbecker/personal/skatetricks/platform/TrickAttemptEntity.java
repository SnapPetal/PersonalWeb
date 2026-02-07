package biz.thonbecker.personal.skatetricks.platform;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trick_attempts", schema = "skatetricks")
@Data
@NoArgsConstructor
class TrickAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "trick_name", length = 100)
    private String trickName;

    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "form_score")
    private Integer formScore;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
