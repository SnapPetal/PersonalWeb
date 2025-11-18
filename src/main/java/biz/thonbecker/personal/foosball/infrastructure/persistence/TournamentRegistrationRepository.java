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

    // Find registrations for a player
    List<TournamentRegistration> findByPlayerIdOrderByRegistrationDateDesc(Long playerId);

    // Find registrations where player is either primary or partner
    @Query("SELECT r FROM TournamentRegistration r WHERE "
            + "(r.player.id = :playerId OR r.partner.id = :playerId) "
            + "ORDER BY r.registrationDate DESC")
    List<TournamentRegistration> findByPlayerOrPartner(@Param("playerId") Long playerId);

    // Check if player is already registered for tournament
    @Query("SELECT COUNT(r) > 0 FROM TournamentRegistration r WHERE "
            + "r.tournament.id = :tournamentId AND "
            + "(r.player.id = :playerId OR r.partner.id = :playerId) AND "
            + "r.status = 'ACTIVE'")
    boolean isPlayerRegistered(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    // Count active registrations for tournament
    long countByTournamentIdAndStatus(Long tournamentId, TournamentRegistration.RegistrationStatus status);

    // Find registrations with seeds assigned
    @Query("SELECT r FROM TournamentRegistration r WHERE "
            + "r.tournament.id = :tournamentId AND r.seed IS NOT NULL "
            + "ORDER BY r.seed ASC")
    List<TournamentRegistration> findSeededRegistrations(@Param("tournamentId") Long tournamentId);

    // Find registrations without seeds assigned
    @Query("SELECT r FROM TournamentRegistration r WHERE "
            + "r.tournament.id = :tournamentId AND r.seed IS NULL AND r.status = 'ACTIVE' "
            + "ORDER BY r.registrationDate ASC")
    List<TournamentRegistration> findUnseededRegistrations(@Param("tournamentId") Long tournamentId);

    // Find team registrations (with partners)
    @Query("SELECT r FROM TournamentRegistration r WHERE "
            + "r.tournament.id = :tournamentId AND r.partner IS NOT NULL AND r.status = 'ACTIVE' "
            + "ORDER BY r.registrationDate ASC")
    List<TournamentRegistration> findTeamRegistrations(@Param("tournamentId") Long tournamentId);

    // Find individual registrations (without partners)
    @Query("SELECT r FROM TournamentRegistration r WHERE "
            + "r.tournament.id = :tournamentId AND r.partner IS NULL AND r.status = 'ACTIVE' "
            + "ORDER BY r.registrationDate ASC")
    List<TournamentRegistration> findIndividualRegistrations(@Param("tournamentId") Long tournamentId);

    // Get registration with full details
    @Query("SELECT r FROM TournamentRegistration r " + "LEFT JOIN FETCH r.player "
            + "LEFT JOIN FETCH r.partner "
            + "LEFT JOIN FETCH r.tournament "
            + "WHERE r.id = :id")
    Optional<TournamentRegistration> findByIdWithDetails(@Param("id") Long id);
}
