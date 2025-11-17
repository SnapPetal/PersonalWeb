package biz.thonbecker.personal.tankgame.infrastructure.persistence;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "player_progression", schema = "tankgame")
@Data
@NoArgsConstructor
public class PlayerProgressionEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "current_xp", nullable = false)
    private Long currentXp = 0L;

    @Column(name = "total_xp", nullable = false)
    private Long totalXp = 0L;

    @Column(name = "coins", nullable = false)
    private Long coins = 0L;

    @Column(name = "total_kills", nullable = false)
    private Integer totalKills = 0;

    @Column(name = "total_deaths", nullable = false)
    private Integer totalDeaths = 0;

    @Column(name = "total_wins", nullable = false)
    private Integer totalWins = 0;

    @Column(name = "total_games", nullable = false)
    private Integer totalGames = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
