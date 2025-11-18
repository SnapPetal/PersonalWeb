package biz.thonbecker.personal.tankgame.domain;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerProgression {
    private String userId;
    private String username;
    private int level = 1;
    private long currentXp = 0;
    private long totalXp = 0;
    private long coins = 0;
    private int totalKills = 0;
    private int totalDeaths = 0;
    private int totalWins = 0;
    private int totalGames = 0;
    private Instant createdAt;
    private Instant updatedAt;

    public PlayerProgression(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.level = 1;
        this.currentXp = 0;
        this.totalXp = 0;
        this.coins = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Calculate XP required for next level.
     * Formula: 100 * level^1.5
     * Level 1->2: 100 XP
     * Level 2->3: 283 XP
     * Level 10->11: 3,162 XP
     */
    public long getXpForNextLevel() {
        return (long) (100 * Math.pow(level, 1.5));
    }

    /**
     * Calculate XP required for current level
     */
    public long getXpForCurrentLevel() {
        if (level == 1) return 0;
        return (long) (100 * Math.pow(level - 1, 1.5));
    }

    /**
     * Get progress to next level as percentage (0-100)
     */
    public double getProgressPercent() {
        long xpForCurrent = getXpForCurrentLevel();
        long xpForNext = getXpForNextLevel();
        long xpNeeded = xpForNext - xpForCurrent;
        return (currentXp * 100.0) / xpNeeded;
    }

    /**
     * Add XP and handle leveling up.
     * Returns number of levels gained.
     */
    public int addXp(long xpGained) {
        this.currentXp += xpGained;
        this.totalXp += xpGained;

        int levelsGained = 0;

        // Check for level ups (can level multiple times with large XP gains)
        while (currentXp >= getXpForNextLevel()) {
            levelUp();
            levelsGained++;
        }

        this.updatedAt = Instant.now();
        return levelsGained;
    }

    private void levelUp() {
        this.level++;
        this.currentXp = 0; // Reset current XP for new level

        // Award coins on level up
        this.coins += level * 10; // 10 coins per level
    }

    public void addKill() {
        this.totalKills++;
        this.updatedAt = Instant.now();
    }

    public void addDeath() {
        this.totalDeaths++;
        this.updatedAt = Instant.now();
    }

    public void addWin() {
        this.totalWins++;
        this.updatedAt = Instant.now();
    }

    public void addGame() {
        this.totalGames++;
        this.updatedAt = Instant.now();
    }

    public void addCoins(long amount) {
        this.coins += amount;
        this.updatedAt = Instant.now();
    }

    public double getKillDeathRatio() {
        if (totalDeaths == 0) return totalKills;
        return (double) totalKills / totalDeaths;
    }

    public double getWinRate() {
        if (totalGames == 0) return 0;
        return (totalWins * 100.0) / totalGames;
    }
}
