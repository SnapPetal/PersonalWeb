package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentStandingRepository extends JpaRepository<TournamentStanding, Long> {

    // Find standings ordered by points (for recalculation)
    @Query("SELECT s FROM TournamentStanding s WHERE s.tournament.id = :tournamentId "
            + "ORDER BY s.points DESC, s.goalDifference DESC, s.goalsFor DESC, s.gamesPlayed ASC")
    List<TournamentStanding> findByTournamentIdOrderByPointsDesc(@Param("tournamentId") Long tournamentId);

    // Find standing for specific registration
    Optional<TournamentStanding> findByTournamentIdAndRegistrationId(Long tournamentId, Long registrationId);
}
