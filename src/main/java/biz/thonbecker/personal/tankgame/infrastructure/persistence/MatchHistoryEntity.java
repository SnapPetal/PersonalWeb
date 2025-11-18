package biz.thonbecker.personal.tankgame.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_history", schema = "tankgame")
@Data
@NoArgsConstructor
public class MatchHistoryEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "placement", nullable = false)
    private Integer placement;

    @Column(name = "kills", nullable = false)
    private Integer kills = 0;

    @Column(name = "deaths", nullable = false)
    private Integer deaths = 1;

    @Column(name = "damage_dealt", nullable = false)
    private Integer damageDealt = 0;

    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned = 0;

    @Column(name = "coins_earned", nullable = false)
    private Integer coinsEarned = 0;

    @Column(name = "match_duration_seconds")
    private Integer matchDurationSeconds;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @PrePersist
    protected void onCreate() {
        if (playedAt == null) {
            playedAt = Instant.now();
        }
    }
}
