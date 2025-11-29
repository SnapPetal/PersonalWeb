package biz.thonbecker.personal.foosball.infrastructure.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Records historical rating changes for players.
 * Allows tracking rating progression over time and generating charts.
 */
@Getter
@Setter
@ToString(exclude = {"player", "game"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(
        name = "rating_history",
        schema = "foosball",
        indexes = {
            @Index(name = "idx_rating_history_player", columnList = "player_id"),
            @Index(name = "idx_rating_history_recorded_at", columnList = "recorded_at")
        })
@EntityListeners(AuditingEntityListener.class)
public class RatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @NotNull(message = "Player is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @NotNull(message = "Old rating is required")
    @Column(name = "old_rating", nullable = false)
    private Integer oldRating;

    @NotNull(message = "New rating is required")
    @Column(name = "new_rating", nullable = false)
    private Integer newRating;

    @NotNull(message = "Change is required")
    @Column(name = "change", nullable = false)
    private Integer change;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private @Nullable Game game;

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    public RatingHistory(Player player, Integer oldRating, Integer newRating, Integer change, Game game) {
        this.player = player;
        this.oldRating = oldRating;
        this.newRating = newRating;
        this.change = change;
        this.game = game;
    }
}
