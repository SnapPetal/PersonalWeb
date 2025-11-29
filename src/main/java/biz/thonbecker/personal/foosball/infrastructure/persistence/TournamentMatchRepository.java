package biz.thonbecker.personal.foosball.infrastructure.persistence;

import biz.thonbecker.personal.foosball.infrastructure.web.model.BracketViewDto;
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

    // Find bracket view for tournament
    @Query("SELECT new biz.thonbecker.personal.foosball.infrastructure.web.model.BracketViewDto("
            + "m.id, m.roundNumber, m.matchNumber, m.bracketType, "
            + "CASE WHEN m.team1 IS NOT NULL THEN "
            + "  CASE WHEN m.team1.teamName IS NOT NULL AND TRIM(m.team1.teamName) <> '' THEN m.team1.teamName "
            + "       WHEN m.team1.partner IS NOT NULL THEN CONCAT(t1p.name, ' & ', t1partner.name) "
            + "       ELSE t1p.name END "
            + "ELSE NULL END, "
            + "CASE WHEN m.team2 IS NOT NULL THEN "
            + "  CASE WHEN m.team2.teamName IS NOT NULL AND TRIM(m.team2.teamName) <> '' THEN m.team2.teamName "
            + "       WHEN m.team2.partner IS NOT NULL THEN CONCAT(t2p.name, ' & ', t2partner.name) "
            + "       ELSE t2p.name END "
            + "ELSE NULL END, "
            + "CASE WHEN m.winner IS NOT NULL THEN "
            + "  CASE WHEN m.winner.teamName IS NOT NULL AND TRIM(m.winner.teamName) <> '' THEN m.winner.teamName "
            + "       WHEN m.winner.partner IS NOT NULL THEN CONCAT(wp.name, ' & ', wpartner.name) "
            + "       ELSE wp.name END "
            + "ELSE NULL END, "
            + "m.status, m.scheduledTime, m.completedAt, "
            + "CASE WHEN m.nextMatch IS NOT NULL THEN m.nextMatch.id ELSE NULL END, "
            + "CASE WHEN m.consolationMatch IS NOT NULL THEN m.consolationMatch.id ELSE NULL END) "
            + "FROM TournamentMatch m "
            + "LEFT JOIN m.team1 t1 "
            + "LEFT JOIN t1.player t1p "
            + "LEFT JOIN t1.partner t1partner "
            + "LEFT JOIN m.team2 t2 "
            + "LEFT JOIN t2.player t2p "
            + "LEFT JOIN t2.partner t2partner "
            + "LEFT JOIN m.winner w "
            + "LEFT JOIN w.player wp "
            + "LEFT JOIN w.partner wpartner "
            + "WHERE m.tournament.id = :tournamentId "
            + "ORDER BY m.roundNumber ASC, m.matchNumber ASC")
    List<BracketViewDto> findBracketView(@Param("tournamentId") Long tournamentId);

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
}
