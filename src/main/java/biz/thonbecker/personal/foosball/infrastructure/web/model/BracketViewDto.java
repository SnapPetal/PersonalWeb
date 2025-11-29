package biz.thonbecker.personal.foosball.infrastructure.web.model;

import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentMatch;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record BracketViewDto(
        Long matchId,
        Integer roundNumber,
        Integer matchNumber,
        TournamentMatch.BracketType bracketType,
        @Nullable String team1DisplayName,
        @Nullable String team2DisplayName,
        @Nullable String winnerDisplayName,
        TournamentMatch.MatchStatus status,
        @Nullable Instant scheduledTime,
        @Nullable Instant completedAt,
        @Nullable Long nextMatchId,
        @Nullable Long consolationMatchId) {}
