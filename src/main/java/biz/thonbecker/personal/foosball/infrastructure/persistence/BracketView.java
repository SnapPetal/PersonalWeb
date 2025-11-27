package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.time.Instant;

/**
 * Projection for tournament bracket information
 */
public interface BracketView {
    Long getMatchId();

    Integer getRoundNumber();

    Integer getMatchNumber();

    TournamentMatch.BracketType getBracketType();

    String getTeam1DisplayName();

    String getTeam2DisplayName();

    String getWinnerDisplayName();

    TournamentMatch.MatchStatus getStatus();

    Instant getScheduledTime();

    Instant getCompletedAt();

    Long getNextMatchId();

    Long getConsolationMatchId();
}
