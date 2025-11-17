package biz.thonbecker.personal.tankgame.infrastructure.persistence;

import biz.thonbecker.personal.tankgame.domain.MatchResult;
import biz.thonbecker.personal.tankgame.domain.PlayerProgression;

public class ProgressionMapper {

    public static PlayerProgression toDomain(PlayerProgressionEntity entity) {
        PlayerProgression progression = new PlayerProgression();
        progression.setUserId(entity.getUserId());
        progression.setUsername(entity.getUsername());
        progression.setLevel(entity.getLevel());
        progression.setCurrentXp(entity.getCurrentXp());
        progression.setTotalXp(entity.getTotalXp());
        progression.setCoins(entity.getCoins());
        progression.setTotalKills(entity.getTotalKills());
        progression.setTotalDeaths(entity.getTotalDeaths());
        progression.setTotalWins(entity.getTotalWins());
        progression.setTotalGames(entity.getTotalGames());
        progression.setCreatedAt(entity.getCreatedAt());
        progression.setUpdatedAt(entity.getUpdatedAt());
        return progression;
    }

    public static PlayerProgressionEntity toEntity(PlayerProgression progression) {
        PlayerProgressionEntity entity = new PlayerProgressionEntity();
        entity.setUserId(progression.getUserId());
        entity.setUsername(progression.getUsername());
        entity.setLevel(progression.getLevel());
        entity.setCurrentXp(progression.getCurrentXp());
        entity.setTotalXp(progression.getTotalXp());
        entity.setCoins(progression.getCoins());
        entity.setTotalKills(progression.getTotalKills());
        entity.setTotalDeaths(progression.getTotalDeaths());
        entity.setTotalWins(progression.getTotalWins());
        entity.setTotalGames(progression.getTotalGames());
        entity.setCreatedAt(progression.getCreatedAt());
        entity.setUpdatedAt(progression.getUpdatedAt());
        return entity;
    }

    public static MatchHistoryEntity toEntity(MatchResult result) {
        MatchHistoryEntity entity = new MatchHistoryEntity();
        entity.setId(result.getId());
        entity.setGameId(result.getGameId());
        entity.setUserId(result.getUserId());
        entity.setUsername(result.getUsername());
        entity.setPlacement(result.getPlacement());
        entity.setKills(result.getKills());
        entity.setDeaths(result.getDeaths());
        entity.setDamageDealt(result.getDamageDealt());
        entity.setXpEarned(result.getXpEarned());
        entity.setCoinsEarned(result.getCoinsEarned());
        entity.setMatchDurationSeconds(result.getMatchDurationSeconds());
        entity.setPlayedAt(result.getPlayedAt());
        return entity;
    }

    public static MatchResult toDomain(MatchHistoryEntity entity) {
        MatchResult result = new MatchResult();
        result.setId(entity.getId());
        result.setGameId(entity.getGameId());
        result.setUserId(entity.getUserId());
        result.setUsername(entity.getUsername());
        result.setPlacement(entity.getPlacement());
        result.setKills(entity.getKills());
        result.setDeaths(entity.getDeaths());
        result.setDamageDealt(entity.getDamageDealt());
        result.setXpEarned(entity.getXpEarned());
        result.setCoinsEarned(entity.getCoinsEarned());
        result.setMatchDurationSeconds(entity.getMatchDurationSeconds());
        result.setPlayedAt(entity.getPlayedAt());
        return result;
    }
}
