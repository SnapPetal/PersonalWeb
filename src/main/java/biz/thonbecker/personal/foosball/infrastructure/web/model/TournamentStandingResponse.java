package biz.thonbecker.personal.foosball.infrastructure.web.model;

import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentStanding;
import java.math.BigDecimal;
import java.time.Instant;

public record TournamentStandingResponse(
        Long id,
        Long tournamentId,
        String displayName,
        Integer position,
        BigDecimal points,
        Integer wins,
        Integer losses,
        Integer draws,
        Integer gamesPlayed,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference,
        Double winPercentage,
        Double pointsPerGame,
        Double goalsPerGame,
        String form,
        String summary,
        Instant updatedAt) {
    public static TournamentStandingResponse fromEntity(TournamentStanding standing) {
        return new TournamentStandingResponse(
                standing.getId(),
                standing.getTournament().getId(),
                standing.getDisplayName(),
                standing.getPosition(),
                standing.getPoints(),
                standing.getWins(),
                standing.getLosses(),
                standing.getDraws(),
                standing.getGamesPlayed(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDifference(),
                standing.getWinPercentage(),
                standing.getPointsPerGame(),
                standing.getGoalsPerGame(),
                standing.getForm(),
                standing.getSummary(),
                standing.getUpdatedAt());
    }
}
