package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    // Find tournaments by status
    List<Tournament> findByStatus(Tournament.TournamentStatus status);

    // Find tournaments by type
    List<Tournament> findByTournamentType(Tournament.TournamentType type);

    // Find tournaments created by a specific player
    List<Tournament> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

    // Find active tournaments (not cancelled or completed)
    @Query("SELECT t FROM Tournament t WHERE t.status NOT IN ('CANCELLED', 'COMPLETED') ORDER BY t.createdAt DESC")
    List<Tournament> findActiveTournaments();

    // Find tournaments where registration is currently open
    @Query("SELECT t FROM Tournament t WHERE t.status = 'REGISTRATION_OPEN' "
            + "AND (t.registrationStart IS NULL OR t.registrationStart <= :now) "
            + "AND (t.registrationEnd IS NULL OR t.registrationEnd > :now)")
    List<Tournament> findTournamentsWithOpenRegistration(@Param("now") LocalDateTime now);

    // Find upcoming tournaments
    @Query("SELECT t FROM Tournament t WHERE t.startDate > :now ORDER BY t.startDate ASC")
    List<Tournament> findUpcomingTournaments(@Param("now") LocalDateTime now);

    // Find tournaments a player is registered for
    @Query("SELECT DISTINCT t FROM Tournament t JOIN t.registrations r "
            + "WHERE (r.player.id = :playerId OR r.partner.id = :playerId) "
            + "AND r.status = 'ACTIVE' ORDER BY t.startDate ASC")
    List<Tournament> findTournamentsForPlayer(@Param("playerId") Long playerId);

    // Tournament summary projection
    @Query("SELECT t.id as id, t.name as name, t.description as description, "
            + "t.tournamentType as tournamentType, t.status as status, "
            + "t.maxParticipants as maxParticipants, t.registrationStart as registrationStart, "
            + "t.registrationEnd as registrationEnd, t.startDate as startDate, "
            + "t.endDate as endDate, MAX(t.createdBy.name) as createdByName, "
            + "t.createdAt as createdAt, "
            + "COUNT(r) as registrationsCount, "
            + "COUNT(CASE WHEN r.status = 'ACTIVE' THEN 1 END) as activeRegistrationsCount "
            + "FROM Tournament t LEFT JOIN t.registrations r "
            + "GROUP BY t.id, t.name, t.description, t.tournamentType, t.status, "
            + "t.maxParticipants, t.registrationStart, t.registrationEnd, "
            + "t.startDate, t.endDate, t.createdAt "
            + "ORDER BY t.createdAt DESC")
    Page<TournamentSummary> findTournamentSummaries(Pageable pageable);

    // Search tournaments by name
    @Query("SELECT t FROM Tournament t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "ORDER BY t.createdAt DESC")
    List<Tournament> searchByName(@Param("search") String search);

    // Find tournament with full details
    @Query("SELECT t FROM Tournament t " + "LEFT JOIN FETCH t.registrations r "
            + "LEFT JOIN FETCH r.player "
            + "LEFT JOIN FETCH r.partner "
            + "WHERE t.id = :id")
    Optional<Tournament> findByIdWithRegistrations(@Param("id") Long id);

    // Find tournament with matches
    @Query("SELECT t FROM Tournament t " + "LEFT JOIN FETCH t.matches m "
            + "LEFT JOIN FETCH m.team1 "
            + "LEFT JOIN FETCH m.team2 "
            + "LEFT JOIN FETCH m.winner "
            + "LEFT JOIN FETCH m.game "
            + "WHERE t.id = :id")
    Optional<Tournament> findByIdWithMatches(@Param("id") Long id);

    // Statistics queries
    @Query("SELECT COUNT(t) FROM Tournament t WHERE t.status = 'COMPLETED'")
    long countCompletedTournaments();

    @Query("SELECT COUNT(t) FROM Tournament t WHERE t.status = 'IN_PROGRESS'")
    long countActiveTournaments();

    @Query("SELECT t.tournamentType, COUNT(t) FROM Tournament t GROUP BY t.tournamentType")
    List<Object[]> getTournamentTypeStatistics();
}
