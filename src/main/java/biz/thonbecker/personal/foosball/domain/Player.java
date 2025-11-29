package biz.thonbecker.personal.foosball.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Represents a Foosball player in the domain model.
 * Players can participate in games and have associated statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String name;
    private int rating;
    private @Nullable Integer peakRating;
    private int gamesPlayed;
    private int currentStreak;
    private @Nullable Integer bestStreak;

    public Player(String name) {
        this.name = name;
        this.rating = 1000;
        this.gamesPlayed = 0;
        this.currentStreak = 0;
    }

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.rating = 1000;
        this.gamesPlayed = 0;
        this.currentStreak = 0;
    }

    public RankTier getRankTier() {
        return RankTier.fromRating(this.rating);
    }
}
