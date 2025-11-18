package biz.thonbecker.personal.tankgame.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class MatchResult {
    private String id;
    private String gameId;
    private String userId;
    private String username;
    private int placement; // 1st, 2nd, 3rd, 4th
    private int kills;
    private int deaths;
    private int damageDealt;
    private int xpEarned;
    private int coinsEarned;
    private Integer matchDurationSeconds;
    private Instant playedAt;

    public MatchResult() {
        this.id = UUID.randomUUID().toString();
        this.playedAt = Instant.now();
        this.deaths = 1; // Minimum 1 death per game (when eliminated)
    }

    public MatchResult(String gameId, String userId, String username, int placement, int kills) {
        this();
        this.gameId = gameId;
        this.userId = userId;
        this.username = username;
        this.placement = placement;
        this.kills = kills;
        calculateRewards();
    }

    /**
     * Calculate XP and coins earned based on performance.
     *
     * XP Formula:
     * - Base: 50 XP for participating
     * - Placement: 100 XP for 1st, 60 for 2nd, 30 for 3rd, 10 for 4th
     * - Kills: 20 XP per kill
     * - Survival bonus: Extra XP for top 2
     *
     * Coins Formula:
     * - Base: 10 coins
     * - Placement: 50 for 1st, 30 for 2nd, 15 for 3rd, 5 for 4th
     * - Kills: 5 coins per kill
     */
    public void calculateRewards() {
        // XP Calculation
        int baseXp = 50;
        int placementXp =
                switch (placement) {
                    case 1 -> 100;
                    case 2 -> 60;
                    case 3 -> 30;
                    default -> 10;
                };
        int killXp = kills * 20;
        int survivalBonus = placement <= 2 ? 50 : 0;

        this.xpEarned = baseXp + placementXp + killXp + survivalBonus;

        // Coins Calculation
        int baseCoins = 10;
        int placementCoins =
                switch (placement) {
                    case 1 -> 50;
                    case 2 -> 30;
                    case 3 -> 15;
                    default -> 5;
                };
        int killCoins = kills * 5;

        this.coinsEarned = baseCoins + placementCoins + killCoins;
    }
}
