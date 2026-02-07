package biz.thonbecker.personal.foosball.platform.persistence;

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

    // Find active tournaments (not cancelled or completed)
    @Query("SELECT t FROM Tournament t WHERE t.status NOT IN ('CANCELLED', 'COMPLETED') ORDER BY t.createdAt DESC")
    List<Tournament> findActiveTournaments();

    // Find tournaments a player is registered for
    @Query("SELECT DISTINCT t FROM Tournament t JOIN t.registrations r "
            + "WHERE (r.player.id = :playerId OR r.partner.id = :playerId) "
            + "AND r.status = 'ACTIVE' ORDER BY t.startDate ASC")
    List<Tournament> findTournamentsForPlayer(@Param("playerId") Long playerId);

    // Tournament summary projection
    @Query("SELECT new biz.thonbecker.personal.foosball.platform.web.model.TournamentSummaryDto("
            + "t.id, t.name, t.description, t.tournamentType, t.status, "
            + "t.maxParticipants, t.registrationStart, t.registrationEnd, "
            + "t.startDate, t.endDate, cb.name, t.createdAt, "
            + "COUNT(r), "
            + "COUNT(CASE WHEN r.status = 'ACTIVE' THEN 1 END)) "
            + "FROM Tournament t LEFT JOIN t.registrations r "
            + "LEFT JOIN t.createdBy cb "
            + "GROUP BY t.id, t.name, t.description, t.tournamentType, t.status, "
            + "t.maxParticipants, t.registrationStart, t.registrationEnd, "
            + "t.startDate, t.endDate, cb.name, t.createdAt "
            + "ORDER BY t.createdAt DESC")
    Page<biz.thonbecker.personal.foosball.platform.web.model.TournamentSummaryDto> findTournamentSummaries(
            Pageable pageable);

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
}
