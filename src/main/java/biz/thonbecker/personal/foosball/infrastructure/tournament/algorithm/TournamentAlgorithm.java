package biz.thonbecker.personal.foosball.infrastructure.tournament.algorithm;

import biz.thonbecker.personal.foosball.infrastructure.persistence.Tournament;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentMatch;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentRegistration;
import java.util.List;

/**
 * Interface for tournament bracket generation algorithms
 */
public interface TournamentAlgorithm {

    /**
     * Generate the initial bracket for the tournament
     * @param tournament The tournament to generate bracket for
     * @param registrations List of active registrations
     * @return List of matches representing the bracket structure
     */
    List<TournamentMatch> generateBracket(Tournament tournament, List<TournamentRegistration> registrations);

    /**
     * Advance winners to next round after a match is completed
     * @param completedMatch The match that was just completed
     * @return List of matches that were updated/created as a result
     */
    List<TournamentMatch> advanceWinner(TournamentMatch completedMatch);

    /**
     * Check if the tournament is complete
     * @param tournament The tournament to check
     * @return true if tournament is complete, false otherwise
     */
    boolean isTournamentComplete(Tournament tournament);

    /**
     * Get the minimum number of participants required for this tournament type
     * @return minimum participants
     */
    int getMinimumParticipants();

    /**
     * Validate if the number of participants is valid for this tournament type
     * @param participantCount number of participants
     * @return true if valid, false otherwise
     */
    boolean isValidParticipantCount(int participantCount);

    /**
     * Get the tournament type this algorithm handles
     * @return tournament type
     */
    Tournament.TournamentType getTournamentType();
}
