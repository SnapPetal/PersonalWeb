package biz.thonbecker.personal.foosball.api;

import biz.thonbecker.personal.foosball.domain.GameResult;
import java.time.Instant;

/**
 * Domain event published when a foosball game is recorded.
 * Other modules can listen to this event to perform actions like updating statistics,
 * triggering notifications, or maintaining leaderboards.
 */
public record GameRecordedEvent(
        Long gameId,
        String team1Name,
        int team1Score,
        String team2Name,
        int team2Score,
        GameResult result,
        String winnerTeamName,
        Instant recordedAt) {}
