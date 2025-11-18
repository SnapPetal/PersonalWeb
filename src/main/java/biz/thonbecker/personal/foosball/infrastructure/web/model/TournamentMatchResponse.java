package biz.thonbecker.personal.foosball.infrastructure.web.model;

import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentMatch;
import java.time.LocalDateTime;

public record TournamentMatchResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        Integer roundNumber,
        Integer matchNumber,
        TournamentMatch.BracketType bracketType,
        String team1DisplayName,
        String team2DisplayName,
        String winnerDisplayName,
        Long gameId,
        TournamentMatch.MatchStatus status,
        LocalDateTime scheduledTime,
        LocalDateTime completedAt,
        String displayName,
        String matchDescription,
        boolean isReady,
        boolean isCompleted,
        boolean hasBye,
        String byeWinnerName) {
    public static TournamentMatchResponse fromEntity(TournamentMatch match) {
        return new TournamentMatchResponse(
                match.getId(),
                match.getTournament().getId(),
                match.getTournament().getName(),
                match.getRoundNumber(),
                match.getMatchNumber(),
                match.getBracketType(),
                match.getTeam1() != null ? match.getTeam1().getDisplayName() : null,
                match.getTeam2() != null ? match.getTeam2().getDisplayName() : null,
                match.getWinner() != null ? match.getWinner().getDisplayName() : null,
                match.getGame() != null ? match.getGame().getId() : null,
                match.getStatus(),
                match.getScheduledTime(),
                match.getCompletedAt(),
                match.getDisplayName(),
                match.getMatchDescription(),
                match.isReady(),
                match.isCompleted(),
                match.hasBye(),
                match.getByeWinner() != null ? match.getByeWinner().getDisplayName() : null);
    }
}
