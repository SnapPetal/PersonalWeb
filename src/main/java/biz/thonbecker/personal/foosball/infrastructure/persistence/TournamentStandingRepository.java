package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentStandingRepository extends JpaRepository<TournamentStanding, Long> {

    // Find standings for tournament ordered by position
    List<TournamentStanding> findByTournamentIdOrderByPositionAsc(Long tournamentId);

    // Find standings ordered by points (for recalculation)
    @Query("SELECT s FROM TournamentStanding s WHERE s.tournament.id = :tournamentId "
            + "ORDER BY s.points DESC, s.goalDifference DESC, s.goalsFor DESC, s.gamesPlayed ASC")
    List<TournamentStanding> findByTournamentIdOrderByPointsDesc(@Param("tournamentId") Long tournamentId);

    // Find standing for specific registration
    Optional<TournamentStanding> findByTournamentIdAndRegistrationId(Long tournamentId, Long registrationId);

    // Get leaderboard view
    @Query("SELECT s.registration.id as registrationId, "
            + "CASE WHEN s.registration.teamName IS NOT NULL THEN s.registration.teamName "
            + "     WHEN s.registration.partner IS NOT NULL THEN CONCAT(s.registration.player.name, ' & ', s.registration.partner.name) "
            + "     ELSE s.registration.player.name END as displayName, "
            + "s.position as position, s.points as points, s.wins as wins, s.losses as losses, "
            + "s.draws as draws, s.gamesPlayed as gamesPlayed, s.goalsFor as goalsFor, "
            + "s.goalsAgainst as goalsAgainst, s.goalDifference as goalDifference "
            + "FROM TournamentStanding s WHERE s.tournament.id = :tournamentId "
            + "ORDER BY s.position ASC NULLS LAST, s.points DESC, s.goalDifference DESC")
    List<LeaderboardView> findLeaderboard(@Param("tournamentId") Long tournamentId);

    // Get top N standings
    @Query("SELECT s FROM TournamentStanding s WHERE s.tournament.id = :tournamentId "
            + "ORDER BY s.position ASC NULLS LAST, s.points DESC, s.goalDifference DESC")
    List<TournamentStanding> findTopStandings(
            @Param("tournamentId") Long tournamentId, org.springframework.data.domain.Pageable pageable);

    // Find standings with games played
    @Query("SELECT s FROM TournamentStanding s WHERE s.tournament.id = :tournamentId "
            + "AND s.gamesPlayed > 0 "
            + "ORDER BY s.points DESC, s.goalDifference DESC, s.goalsFor DESC")
    List<TournamentStanding> findStandingsWithGamesPlayed(@Param("tournamentId") Long tournamentId);

    // Get tournament statistics
    @Query("SELECT COUNT(s), AVG(s.points), AVG(s.gamesPlayed), "
            + "AVG(CAST(s.goalsFor AS double)), AVG(CAST(s.goalsAgainst AS double)) "
            + "FROM TournamentStanding s WHERE s.tournament.id = :tournamentId")
    Object[] getTournamentStatistics(@Param("tournamentId") Long tournamentId);

    // Find standing with details
    @Query("SELECT s FROM TournamentStanding s " + "LEFT JOIN FETCH s.registration r "
            + "LEFT JOIN FETCH r.player "
            + "LEFT JOIN FETCH r.partner "
            + "WHERE s.id = :id")
    Optional<TournamentStanding> findByIdWithDetails(@Param("id") Long id);

    // Delete all standings for tournament (for recalculation)
    void deleteByTournamentId(Long tournamentId);

    // Check if standings exist for tournament
    boolean existsByTournamentId(Long tournamentId);
}
