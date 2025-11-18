package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

    // Find matches for a tournament ordered by round and match number
    List<TournamentMatch> findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(Long tournamentId);

    // Find matches for specific round
    List<TournamentMatch> findByTournamentIdAndRoundNumberOrderByMatchNumberAsc(Long tournamentId, Integer roundNumber);

    // Find matches by status
    List<TournamentMatch> findByTournamentIdAndStatusOrderByRoundNumberAscMatchNumberAsc(
            Long tournamentId, TournamentMatch.MatchStatus status);

    // Find matches for a specific registration (team)
    @Query("SELECT m FROM TournamentMatch m WHERE " + "m.tournament.id = :tournamentId AND "
            + "(m.team1.id = :registrationId OR m.team2.id = :registrationId) "
            + "ORDER BY m.roundNumber ASC, m.matchNumber ASC")
    List<TournamentMatch> findMatchesForRegistration(
            @Param("tournamentId") Long tournamentId, @Param("registrationId") Long registrationId);

    // Find next match for winner
    @Query("SELECT m FROM TournamentMatch m WHERE m.nextMatch.id = :matchId")
    List<TournamentMatch> findMatchesAdvancingTo(@Param("matchId") Long matchId);

    // Find bracket view for tournament
    @Query("SELECT m.id as matchId, m.roundNumber as roundNumber, m.matchNumber as matchNumber, "
            + "m.bracketType as bracketType, "
            + "CASE WHEN m.team1 IS NOT NULL THEN "
            + "  CASE WHEN m.team1.teamName IS NOT NULL AND TRIM(m.team1.teamName) <> '' THEN m.team1.teamName "
            + "       WHEN m.team1.partner IS NOT NULL THEN CONCAT(m.team1.player.name, ' & ', m.team1.partner.name) "
            + "       ELSE m.team1.player.name END "
            + "ELSE NULL END as team1DisplayName, "
            + "CASE WHEN m.team2 IS NOT NULL THEN "
            + "  CASE WHEN m.team2.teamName IS NOT NULL AND TRIM(m.team2.teamName) <> '' THEN m.team2.teamName "
            + "       WHEN m.team2.partner IS NOT NULL THEN CONCAT(m.team2.player.name, ' & ', m.team2.partner.name) "
            + "       ELSE m.team2.player.name END "
            + "ELSE NULL END as team2DisplayName, "
            + "CASE WHEN m.winner IS NOT NULL THEN "
            + "  CASE WHEN m.winner.teamName IS NOT NULL AND TRIM(m.winner.teamName) <> '' THEN m.winner.teamName "
            + "       WHEN m.winner.partner IS NOT NULL THEN CONCAT(m.winner.player.name, ' & ', m.winner.partner.name) "
            + "       ELSE m.winner.player.name END "
            + "ELSE NULL END as winnerDisplayName, "
            + "m.status as status, m.scheduledTime as scheduledTime, m.completedAt as completedAt, "
            + "CASE WHEN m.nextMatch IS NOT NULL THEN m.nextMatch.id ELSE NULL END as nextMatchId, "
            + "CASE WHEN m.consolationMatch IS NOT NULL THEN m.consolationMatch.id ELSE NULL END as consolationMatchId "
            + "FROM TournamentMatch m WHERE m.tournament.id = :tournamentId "
            + "ORDER BY m.roundNumber ASC, m.matchNumber ASC")
    List<BracketView> findBracketView(@Param("tournamentId") Long tournamentId);

    // Find matches by bracket type
    List<TournamentMatch> findByTournamentIdAndBracketTypeOrderByRoundNumberAscMatchNumberAsc(
            Long tournamentId, TournamentMatch.BracketType bracketType);

    // Find ready matches (both teams assigned)
    @Query("SELECT m FROM TournamentMatch m WHERE " + "m.tournament.id = :tournamentId AND "
            + "m.team1 IS NOT NULL AND m.team2 IS NOT NULL AND "
            + "m.status = 'READY' "
            + "ORDER BY m.roundNumber ASC, m.matchNumber ASC")
    List<TournamentMatch> findReadyMatches(@Param("tournamentId") Long tournamentId);

    // Find pending matches (waiting for teams)
    @Query("SELECT m FROM TournamentMatch m WHERE " + "m.tournament.id = :tournamentId AND "
            + "(m.team1 IS NULL OR m.team2 IS NULL) AND "
            + "m.status = 'PENDING' "
            + "ORDER BY m.roundNumber ASC, m.matchNumber ASC")
    List<TournamentMatch> findPendingMatches(@Param("tournamentId") Long tournamentId);

    // Find completed matches
    @Query("SELECT m FROM TournamentMatch m WHERE " + "m.tournament.id = :tournamentId AND "
            + "m.status IN ('COMPLETED', 'WALKOVER') "
            + "ORDER BY m.roundNumber ASC, m.matchNumber ASC")
    List<TournamentMatch> findCompletedMatches(@Param("tournamentId") Long tournamentId);

    // Find match with full details
    @Query("SELECT m FROM TournamentMatch m " + "LEFT JOIN FETCH m.tournament "
            + "LEFT JOIN FETCH m.team1 t1 "
            + "LEFT JOIN FETCH t1.player "
            + "LEFT JOIN FETCH t1.partner "
            + "LEFT JOIN FETCH m.team2 t2 "
            + "LEFT JOIN FETCH t2.player "
            + "LEFT JOIN FETCH t2.partner "
            + "LEFT JOIN FETCH m.winner w "
            + "LEFT JOIN FETCH w.player "
            + "LEFT JOIN FETCH w.partner "
            + "LEFT JOIN FETCH m.game "
            + "WHERE m.id = :id")
    Optional<TournamentMatch> findByIdWithDetails(@Param("id") Long id);

    // Count matches by status for tournament
    @Query("SELECT m.status, COUNT(m) FROM TournamentMatch m WHERE m.tournament.id = :tournamentId GROUP BY m.status")
    List<Object[]> countMatchesByStatus(@Param("tournamentId") Long tournamentId);

    // Find matches in current round
    @Query("SELECT m FROM TournamentMatch m WHERE " + "m.tournament.id = :tournamentId AND "
            + "m.roundNumber = (SELECT MAX(m2.roundNumber) FROM TournamentMatch m2 "
            + "                 WHERE m2.tournament.id = :tournamentId AND "
            + "                 m2.status NOT IN ('COMPLETED', 'WALKOVER')) "
            + "ORDER BY m.matchNumber ASC")
    List<TournamentMatch> findCurrentRoundMatches(@Param("tournamentId") Long tournamentId);

    // Get max round number for tournament
    @Query("SELECT COALESCE(MAX(m.roundNumber), 0) FROM TournamentMatch m WHERE m.tournament.id = :tournamentId")
    Integer getMaxRoundNumber(@Param("tournamentId") Long tournamentId);
}
