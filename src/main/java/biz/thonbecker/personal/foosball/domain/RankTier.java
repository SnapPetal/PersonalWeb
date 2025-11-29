package biz.thonbecker.personal.foosball.domain;

import lombok.Getter;

/**
 * Rank tiers for player progression based on rating.
 * Provides visual feedback and motivation for players.
 */
@Getter
public enum RankTier {
    BRONZE(0, 999, "Bronze", "🥉"),
    SILVER(1000, 1199, "Silver", "🥈"),
    GOLD(1200, 1399, "Gold", "🥇"),
    PLATINUM(1400, 1599, "Platinum", "💎"),
    DIAMOND(1600, 1799, "Diamond", "💠"),
    MASTER(1800, 1999, "Master", "👑"),
    GRANDMASTER(2000, Integer.MAX_VALUE, "Grandmaster", "⭐");

    private final int minRating;
    private final int maxRating;
    private final String name;
    private final String icon;

    RankTier(int minRating, int maxRating, String name, String icon) {
        this.minRating = minRating;
        this.maxRating = maxRating;
        this.name = name;
        this.icon = icon;
    }

    public String getDisplayName() {
        return icon + " " + name;
    }

    /**
     * Get the rank tier for a given rating
     */
    public static RankTier fromRating(int rating) {
        for (RankTier tier : values()) {
            if (rating >= tier.minRating && rating <= tier.maxRating) {
                return tier;
            }
        }
        return BRONZE;
    }

    /**
     * Check if player is at risk of demotion (within 50 points of lower tier)
     */
    public boolean isAtRisk(int currentRating) {
        return currentRating - minRating < 50 && this != BRONZE;
    }

    /**
     * Check if player is close to promotion (within 50 points of next tier)
     */
    public boolean isCloseToPromotion(int currentRating) {
        return maxRating - currentRating < 50 && this != GRANDMASTER;
    }
}
