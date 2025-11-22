package biz.thonbecker.personal.tankgame.application;

import biz.thonbecker.personal.tankgame.domain.MatchResult;
import biz.thonbecker.personal.tankgame.domain.PlayerProgression;
import biz.thonbecker.personal.tankgame.infrastructure.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressionService {

    private final PlayerProgressionRepository progressionRepository;
    private final MatchHistoryRepository matchHistoryRepository;

    /**
     * Get or create player progression
     */
    @Transactional
    public PlayerProgression getOrCreateProgression(String userId, String username) {
        return progressionRepository
                .findByUserId(userId)
                .map(ProgressionMapper::toDomain)
                .orElseGet(() -> {
                    PlayerProgression newProgression = new PlayerProgression(userId, username);
                    PlayerProgressionEntity entity = ProgressionMapper.toEntity(newProgression);
                    PlayerProgressionEntity saved = progressionRepository.save(entity);
                    log.info("Created new progression for user: {} ({})", username, userId);
                    return ProgressionMapper.toDomain(saved);
                });
    }

    /**
     * Record match result and award XP/coins
     */
    @Transactional
    public PlayerProgression recordMatch(MatchResult matchResult) {
        // Get or create progression
        PlayerProgression progression = getOrCreateProgression(matchResult.getUserId(), matchResult.getUsername());

        // Calculate rewards if not already calculated
        if (matchResult.getXpEarned() == 0) {
            matchResult.calculateRewards();
        }

        // Update stats
        progression.addGame();
        for (int i = 0; i < matchResult.getKills(); i++) {
            progression.addKill();
        }

        // Only add death if player didn't win (winner survives)
        if (matchResult.getPlacement() != 1) {
            progression.addDeath();
        }

        if (matchResult.getPlacement() == 1) {
            progression.addWin();
        }

        // Add XP and coins
        int levelsGained = progression.addXp(matchResult.getXpEarned());
        progression.addCoins(matchResult.getCoinsEarned());

        // Save progression
        PlayerProgressionEntity entity = ProgressionMapper.toEntity(progression);
        progressionRepository.save(entity);

        // Save match history
        MatchHistoryEntity matchEntity = ProgressionMapper.toEntity(matchResult);
        matchHistoryRepository.save(matchEntity);

        if (levelsGained > 0) {
            log.info("Player {} leveled up! Now level {}", progression.getUsername(), progression.getLevel());
        }

        log.info(
                "Recorded match for {}: +{} XP, +{} coins, placement: {}",
                progression.getUsername(),
                matchResult.getXpEarned(),
                matchResult.getCoinsEarned(),
                matchResult.getPlacement());

        return progression;
    }

    /**
     * Get player progression by userId
     */
    public Optional<PlayerProgression> getProgression(String userId) {
        return progressionRepository.findByUserId(userId).map(ProgressionMapper::toDomain);
    }

    /**
     * Get player progression by username
     */
    public Optional<PlayerProgression> getProgressionByUsername(String username) {
        return progressionRepository.findByUsername(username).map(ProgressionMapper::toDomain);
    }

    /**
     * Get match history for a player
     */
    public List<MatchResult> getMatchHistory(String userId, int limit) {
        Page<MatchHistoryEntity> page =
                matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId, PageRequest.of(0, limit));

        return page.getContent().stream().map(ProgressionMapper::toDomain).collect(Collectors.toList());
    }

    /**
     * Get leaderboard by total XP
     */
    public List<PlayerProgression> getLeaderboardByXp() {
        return progressionRepository.findTop10ByOrderByTotalXpDesc().stream()
                .map(ProgressionMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get leaderboard by level
     */
    public List<PlayerProgression> getLeaderboardByLevel() {
        return progressionRepository.findTop10ByOrderByLevelDescTotalXpDesc().stream()
                .map(ProgressionMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get leaderboard by kills
     */
    public List<PlayerProgression> getLeaderboardByKills() {
        return progressionRepository.findTop10ByOrderByTotalKillsDesc().stream()
                .map(ProgressionMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get leaderboard by win rate
     */
    public List<PlayerProgression> getLeaderboardByWinRate() {
        return progressionRepository.findTop10ByWinRate().stream()
                .map(ProgressionMapper::toDomain)
                .filter(p -> p.getTotalGames() >= 5) // Minimum 5 games to qualify
                .collect(Collectors.toList());
    }
}
