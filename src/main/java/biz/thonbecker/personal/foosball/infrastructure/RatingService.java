package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.domain.RankTier;
import biz.thonbecker.personal.foosball.infrastructure.persistence.Game;
import biz.thonbecker.personal.foosball.infrastructure.persistence.Player;
import biz.thonbecker.personal.foosball.infrastructure.persistence.PlayerRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.RatingHistory;
import biz.thonbecker.personal.foosball.infrastructure.persistence.RatingHistoryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for calculating and managing player ratings using ELO system.
 * Base rating: 1000
 * K-factor: 32 for regular play, 50 for first 10 games (placement matches)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private static final int INITIAL_RATING = 1000;
    private static final int K_FACTOR_REGULAR = 32;
    private static final int K_FACTOR_PLACEMENT = 50;
    private static final int PLACEMENT_GAMES = 10;
    private static final int GIANT_SLAYER_THRESHOLD = 200;
    private static final int STREAK_BONUS_THRESHOLD = 3;
    private static final int BIG_STREAK_BONUS_THRESHOLD = 5;

    private final PlayerRepository playerRepository;
    private final RatingHistoryRepository ratingHistoryRepository;

    /**
     * Update ratings for all four players after a game completes.
     * Calculates team-based ELO with individual tracking.
     */
    public void updateRatingsAfterGame(Game game) {
        var whitePlayer1 = game.getWhiteTeamPlayer1();
        var whitePlayer2 = game.getWhiteTeamPlayer2();
        var blackPlayer1 = game.getBlackTeamPlayer1();
        var blackPlayer2 = game.getBlackTeamPlayer2();

        log.info(
                "Updating ratings for game: {} vs {} | Winner: {}",
                whitePlayer1.getName() + "/" + whitePlayer2.getName(),
                blackPlayer1.getName() + "/" + blackPlayer2.getName(),
                game.getWinner());

        // Determine winners and losers
        boolean whiteWon = game.getWinner() == Game.TeamColor.WHITE;
        var winner1 = whiteWon ? whitePlayer1 : blackPlayer1;
        var winner2 = whiteWon ? whitePlayer2 : blackPlayer2;
        var loser1 = whiteWon ? blackPlayer1 : whitePlayer1;
        var loser2 = whiteWon ? blackPlayer2 : whitePlayer2;

        // Calculate team ratings (average)
        int winningTeamRating = (winner1.getRating() + winner2.getRating()) / 2;
        int losingTeamRating = (loser1.getRating() + loser2.getRating()) / 2;

        // Calculate the expected outcome
        double expectedWinProbability = calculateExpectedScore(winningTeamRating, losingTeamRating);

        // Base points for this game
        int basePoints = calculateBasePoints(expectedWinProbability, winner1, winner2);

        // Calculate individual changes with bonuses
        int winner1Change = calculateIndividualChange(winner1, basePoints, losingTeamRating, true);
        int winner2Change = calculateIndividualChange(winner2, basePoints, losingTeamRating, true);
        int loser1Change = calculateIndividualChange(loser1, -basePoints, winningTeamRating, false);
        int loser2Change = calculateIndividualChange(loser2, -basePoints, winningTeamRating, false);

        // Apply changes
        applyRatingChange(winner1, winner1Change, game);
        applyRatingChange(winner2, winner2Change, game);
        applyRatingChange(loser1, loser1Change, game);
        applyRatingChange(loser2, loser2Change, game);

        // Update streaks
        updateStreak(winner1, true);
        updateStreak(winner2, true);
        updateStreak(loser1, false);
        updateStreak(loser2, false);

        // Save all players
        playerRepository.saveAll(List.of(winner1, winner2, loser1, loser2));

        log.info(
                "Rating changes: {} ({:+d}), {} ({:+d}), {} ({:+d}), {} ({:+d})",
                winner1.getName(),
                winner1Change,
                winner2.getName(),
                winner2Change,
                loser1.getName(),
                loser1Change,
                loser2.getName(),
                loser2Change);
    }

    private int calculateBasePoints(double expectedWinProbability, Player winner1, Player winner2) {
        // Use higher K-factor for placement matches
        int kFactor1 = winner1.getGamesPlayed() < PLACEMENT_GAMES ? K_FACTOR_PLACEMENT : K_FACTOR_REGULAR;
        int kFactor2 = winner2.getGamesPlayed() < PLACEMENT_GAMES ? K_FACTOR_PLACEMENT : K_FACTOR_REGULAR;
        int avgKFactor = (kFactor1 + kFactor2) / 2;

        return (int) Math.round(avgKFactor * (1.0 - expectedWinProbability));
    }

    private int calculateIndividualChange(Player player, int basePoints, int opponentTeamRating, boolean isWinner) {
        int change = basePoints;

        if (isWinner) {
            // Giant Slayer bonus: beat team much higher rated
            if (opponentTeamRating - player.getRating() >= GIANT_SLAYER_THRESHOLD) {
                int giantSlayerBonus = (int) (basePoints * 0.25); // +25% bonus
                change += giantSlayerBonus;
                log.info("Giant Slayer bonus for {}: +{}", player.getName(), giantSlayerBonus);
            }

            // Win streak bonus
            if (player.getCurrentStreak() >= BIG_STREAK_BONUS_THRESHOLD) {
                int streakBonus = (int) (basePoints * 0.2); // +20% for 5+ streak
                change += streakBonus;
                log.info(
                        "Big streak bonus for {}: +{} (streak: {})",
                        player.getName(),
                        streakBonus,
                        player.getCurrentStreak());
            } else if (player.getCurrentStreak() >= STREAK_BONUS_THRESHOLD) {
                int streakBonus = (int) (basePoints * 0.1); // +10% for 3+ streak
                change += streakBonus;
                log.info(
                        "Streak bonus for {}: +{} (streak: {})",
                        player.getName(),
                        streakBonus,
                        player.getCurrentStreak());
            }

            // Comeback bonus: first win after losing streak
            if (player.getCurrentStreak() < -2) {
                int comebackBonus = 5;
                change += comebackBonus;
                log.info("Comeback bonus for {}: +{}", player.getName(), comebackBonus);
            }
        }

        return change;
    }

    private void applyRatingChange(Player player, int change, Game game) {
        int oldRating = player.getRating();
        int newRating = Math.max(0, oldRating + change); // Never go below 0

        player.setRating(newRating);
        player.setGamesPlayed(player.getGamesPlayed() + 1);

        // Track peak rating
        if (player.getPeakRating() == null || newRating > player.getPeakRating()) {
            player.setPeakRating(newRating);
            log.info("New peak rating for {}: {}", player.getName(), newRating);
        }

        // Check for rank change
        RankTier oldTier = RankTier.fromRating(oldRating);
        RankTier newTier = RankTier.fromRating(newRating);
        if (oldTier != newTier) {
            log.info(
                    "Rank change for {}: {} → {}",
                    player.getName(),
                    oldTier.getDisplayName(),
                    newTier.getDisplayName());
        }

        // Record history
        var history = new RatingHistory();
        history.setPlayer(player);
        history.setOldRating(oldRating);
        history.setNewRating(newRating);
        history.setChange(change);
        history.setGame(game);
        ratingHistoryRepository.save(history);
    }

    private void updateStreak(Player player, boolean won) {
        int currentStreak = player.getCurrentStreak();

        if (won) {
            if (currentStreak >= 0) {
                player.setCurrentStreak(currentStreak + 1);
            } else {
                player.setCurrentStreak(1); // Reset to 1 after losing streak
            }
        } else {
            if (currentStreak <= 0) {
                player.setCurrentStreak(currentStreak - 1);
            } else {
                player.setCurrentStreak(-1); // Reset to -1 after winning streak
            }
        }

        // Track best streak
        if (player.getCurrentStreak() > 0) {
            if (player.getBestStreak() == null || player.getCurrentStreak() > player.getBestStreak()) {
                player.setBestStreak(player.getCurrentStreak());
            }
        }
    }

    /**
     * Calculate expected score using ELO formula
     * Returns probability of player A winning (0.0 to 1.0)
     */
    private double calculateExpectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }
}
