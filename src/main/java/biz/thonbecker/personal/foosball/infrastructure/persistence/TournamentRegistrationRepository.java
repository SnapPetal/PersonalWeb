package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {

    // Find registrations for a tournament
    List<TournamentRegistration> findByTournamentIdOrderBySeedAscRegistrationDateAsc(Long tournamentId);

    // Find active registrations for a tournament
    List<TournamentRegistration> findByTournamentIdAndStatusOrderBySeedAscRegistrationDateAsc(
            Long tournamentId, TournamentRegistration.RegistrationStatus status);

    // Find registration by tournament and player
    Optional<TournamentRegistration> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    // Check if player is already registered for tournament
    @Query("SELECT COUNT(r) > 0 FROM TournamentRegistration r WHERE "
            + "r.tournament.id = :tournamentId AND "
            + "(r.player.id = :playerId OR r.partner.id = :playerId) AND "
            + "r.status = 'ACTIVE'")
    boolean isPlayerRegistered(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);
}
