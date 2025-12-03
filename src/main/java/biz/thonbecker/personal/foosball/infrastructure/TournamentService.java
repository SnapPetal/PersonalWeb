package biz.thonbecker.personal.foosball.infrastructure;

import static java.util.Objects.isNull;

import biz.thonbecker.personal.foosball.infrastructure.persistence.Game;
import biz.thonbecker.personal.foosball.infrastructure.persistence.GameRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.PlayerRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.Tournament;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentMatch;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentMatchRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentRegistration;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentRegistrationRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentStanding;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentStandingRepository;
import biz.thonbecker.personal.foosball.infrastructure.tournament.algorithm.DoubleEliminationAlgorithm;
import biz.thonbecker.personal.foosball.infrastructure.tournament.algorithm.SingleEliminationAlgorithm;
import biz.thonbecker.personal.foosball.infrastructure.tournament.algorithm.TournamentAlgorithm;
import biz.thonbecker.personal.foosball.infrastructure.web.model.CreateTournamentRequest;
import biz.thonbecker.personal.foosball.infrastructure.web.model.TournamentRegistrationRequest;
import biz.thonbecker.personal.foosball.infrastructure.web.model.TournamentSummaryDto;
import biz.thonbecker.personal.foosball.infrastructure.web.model.UpdateTournamentRequest;
import biz.thonbecker.personal.foosball.infrastructure.web.model.WalkoverRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentMatchRepository matchRepository;
    private final TournamentStandingRepository standingRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;

    // Tournament algorithms
    private final SingleEliminationAlgorithm singleEliminationAlgorithm;
    private final DoubleEliminationAlgorithm doubleEliminationAlgorithm;

    // Tournament CRUD Operations
    public Tournament createTournament(CreateTournamentRequest request, Long createdById) {
        log.info("Creating tournament: {} by player: {}", request.name(), createdById);

        final var creator = playerRepository
                .findById(createdById)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + createdById));

        // Support single and double elimination
        if (request.tournamentType() != Tournament.TournamentType.SINGLE_ELIMINATION
                && request.tournamentType() != Tournament.TournamentType.DOUBLE_ELIMINATION) {
            throw new UnsupportedOperationException(
                    "Currently only single and double elimination tournaments are supported");
        }

        final var tournament = new Tournament(request.name(), request.tournamentType(), creator);
        tournament.setDescription(request.description());
        tournament.setMaxParticipants(request.maxParticipants());
        tournament.setRegistrationStart(request.registrationStart());
        tournament.setRegistrationEnd(request.registrationEnd());
        tournament.setStartDate(request.startDate());
        tournament.setSettings(request.settings());

        return tournamentRepository.save(tournament);
    }

    public Tournament updateTournament(Long tournamentId, UpdateTournamentRequest request) {
        log.info("Updating tournament: {}", tournamentId);

        final var tournament = getTournamentById(tournamentId);

        // Only allow updates if tournament is still in draft or registration phase
        if (tournament.getStatus() != Tournament.TournamentStatus.DRAFT
                && tournament.getStatus() != Tournament.TournamentStatus.REGISTRATION_OPEN) {
            throw new IllegalStateException("Cannot update tournament that has started");
        }

        // Update fields if provided
        if (request.name() != null) tournament.setName(request.name());
        if (request.description() != null) tournament.setDescription(request.description());
        if (request.tournamentType() != null) {
            if (request.tournamentType() != Tournament.TournamentType.SINGLE_ELIMINATION
                    && request.tournamentType() != Tournament.TournamentType.DOUBLE_ELIMINATION) {
                throw new UnsupportedOperationException(
                        "Currently only single and double elimination tournaments are supported");
            }
            tournament.setTournamentType(request.tournamentType());
        }
        if (request.maxParticipants() != null) tournament.setMaxParticipants(request.maxParticipants());
        if (request.registrationStart() != null) tournament.setRegistrationStart(request.registrationStart());
        if (request.registrationEnd() != null) tournament.setRegistrationEnd(request.registrationEnd());
        if (request.startDate() != null) tournament.setStartDate(request.startDate());
        if (request.settings() != null) tournament.setSettings(request.settings());

        return tournamentRepository.save(tournament);
    }

    public Tournament getTournamentById(Long tournamentId) {
        return tournamentRepository
                .findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with id: " + tournamentId));
    }

    public Tournament getTournamentWithRegistrations(Long tournamentId) {
        return tournamentRepository
                .findByIdWithRegistrations(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with id: " + tournamentId));
    }

    public Page<TournamentSummaryDto> getTournamentSummaries(Pageable pageable) {
        return tournamentRepository.findTournamentSummaries(pageable);
    }

    public List<Tournament> getActiveTournaments() {
        return tournamentRepository.findActiveTournaments();
    }

    public List<Tournament> getTournamentsForPlayer(Long playerId) {
        return tournamentRepository.findTournamentsForPlayer(playerId);
    }

    public void deleteTournament(Long tournamentId) {
        final var tournament = getTournamentById(tournamentId);

        // Only allow deletion if tournament hasn't started
        if (tournament.getStatus() == Tournament.TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == Tournament.TournamentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot delete tournament that has started or completed");
        }

        log.info("Deleting tournament: {}", tournamentId);
        tournamentRepository.delete(tournament);
    }

    // Tournament Status Management
    public Tournament openRegistration(Long tournamentId) {
        log.info("Opening registration for tournament: {}", tournamentId);

        final var tournament = getTournamentById(tournamentId);
        tournament.openRegistration();

        return tournamentRepository.save(tournament);
    }

    public Tournament closeRegistration(Long tournamentId) {
        log.info("Closing registration for tournament: {}", tournamentId);

        final var tournament = getTournamentById(tournamentId);
        tournament.closeRegistration();

        return tournamentRepository.save(tournament);
    }

    public Tournament startTournament(Long tournamentId) {
        log.info("Starting tournament: {}", tournamentId);

        final var tournament = getTournamentWithRegistrations(tournamentId);

        if (!tournament.canStart()) {
            throw new IllegalStateException(
                    "Tournament cannot be started. Check registration status and participant count.");
        }

        // Generate bracket
        generateBracket(tournament);

        tournament.start();
        return tournamentRepository.save(tournament);
    }

    public Tournament cancelTournament(Long tournamentId) {
        log.info("Cancelling tournament: {}", tournamentId);

        final var tournament = getTournamentById(tournamentId);
        tournament.cancel();

        return tournamentRepository.save(tournament);
    }

    // Registration Management
    public TournamentRegistration registerForTournament(Long tournamentId, TournamentRegistrationRequest request) {
        log.info("Registering player {} for tournament {}", request.playerId(), tournamentId);

        final var tournament = getTournamentById(tournamentId);

        if (!tournament.canRegister()) {
            throw new IllegalStateException("Registration is not open for this tournament");
        }

        // Check if player is already registered
        if (registrationRepository.isPlayerRegistered(tournamentId, request.playerId())) {
            throw new IllegalStateException("Player is already registered for this tournament");
        }

        final var player = playerRepository
                .findById(request.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id: " + request.playerId()));

        TournamentRegistration registration;

        if (request.isTeamRegistration()) {
            final var partner = playerRepository
                    .findById(request.partnerId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Partner not found with id: " + request.partnerId()));

            // Check if partner is already registered
            if (registrationRepository.isPlayerRegistered(tournamentId, request.partnerId())) {
                throw new IllegalStateException("Partner is already registered for this tournament");
            }

            registration = new TournamentRegistration(tournament, player, partner, request.teamName());
        } else {
            registration = new TournamentRegistration(tournament, player);
        }

        return registrationRepository.save(registration);
    }

    public void withdrawFromTournament(Long tournamentId, Long playerId) {
        log.info("Withdrawing player {} from tournament {}", playerId, tournamentId);

        final var registration = registrationRepository
                .findByTournamentIdAndPlayerId(tournamentId, playerId)
                .orElseThrow(() -> new EntityNotFoundException("Registration not found"));

        final var tournament = getTournamentById(tournamentId);

        if (tournament.getStatus() == Tournament.TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == Tournament.TournamentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot withdraw from tournament that has started or completed");
        }

        registration.withdraw();
        registrationRepository.save(registration);
    }

    public List<TournamentRegistration> getTournamentRegistrations(Long tournamentId) {
        return registrationRepository.findByTournamentIdOrderBySeedAscRegistrationDateAsc(tournamentId);
    }

    // Bracket and Match Management
    private void generateBracket(Tournament tournament) {
        log.info("Generating bracket for tournament: {}", tournament.getId());

        final var activeRegistrations =
                registrationRepository.findByTournamentIdAndStatusOrderBySeedAscRegistrationDateAsc(
                        tournament.getId(), TournamentRegistration.RegistrationStatus.ACTIVE);

        log.info("Found {} active registrations for tournament {}", activeRegistrations.size(), tournament.getId());
        for (var reg : activeRegistrations) {
            log.info("  Registration: {} (ID: {}, Status: {})", reg.getDisplayName(), reg.getId(), reg.getStatus());
        }

        if (activeRegistrations.isEmpty()) {
            log.error("No active registrations found! Cannot generate bracket.");
            throw new IllegalStateException("No active registrations found for tournament");
        }

        final var algorithm = getTournamentAlgorithm(tournament.getTournamentType());
        final var matches = algorithm.generateBracket(tournament, activeRegistrations);

        log.info("Algorithm generated {} matches", matches.size());
        for (var match : matches) {
            log.info(
                    "  Match {}-{}: {} vs {}",
                    match.getRoundNumber(),
                    match.getMatchNumber(),
                    match.getTeam1().getDisplayName(),
                    match.getTeam2().getDisplayName());
        }

        // Save all matches
        final var savedMatches = matchRepository.saveAll(matches);
        log.info("Saved {} matches to database", savedMatches.size());

        log.info("Generated {} matches for tournament {}", matches.size(), tournament.getId());
    }

    private TournamentAlgorithm getTournamentAlgorithm(Tournament.TournamentType type) {
        return switch (type) {
            case SINGLE_ELIMINATION -> singleEliminationAlgorithm;
            case DOUBLE_ELIMINATION -> doubleEliminationAlgorithm;
            default -> throw new UnsupportedOperationException("Tournament type not supported: " + type);
        };
    }

    public List<TournamentMatch> getTournamentMatches(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(tournamentId);
    }

    public List<biz.thonbecker.personal.foosball.infrastructure.web.model.BracketViewDto> getBracketView(
            Long tournamentId) {
        log.info("Fetching bracket view for tournament: {}", tournamentId);
        final var bracket = matchRepository.findBracketView(tournamentId);
        log.info("Found {} matches in bracket for tournament {}", bracket.size(), tournamentId);
        return bracket;
    }

    public TournamentMatch getMatchById(Long matchId) {
        return matchRepository
                .findByIdWithDetails(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));
    }

    public TournamentMatch completeMatch(Long matchId, Long gameId) {
        log.info("Completing match {} with game {}", matchId, gameId);

        final var match = getMatchById(matchId);
        final var game = gameRepository
                .findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (!match.canStart()) {
            throw new IllegalStateException("Match is not ready to be completed");
        }

        match.complete(game);
        matchRepository.save(match);

        // Update standings
        updateStandingsForMatch(match);

        // Advance winner to next round
        final var algorithm = getTournamentAlgorithm(match.getTournament().getTournamentType());
        final var updatedMatches = algorithm.advanceWinner(match);
        matchRepository.saveAll(updatedMatches);

        // Check if tournament is complete
        if (algorithm.isTournamentComplete(match.getTournament())) {
            final var tournament = match.getTournament();
            tournament.complete();
            tournamentRepository.save(tournament);
            log.info("Tournament {} completed", tournament.getId());
        }

        return match;
    }

    public TournamentMatch recordWalkover(Long matchId, WalkoverRequest request) {
        log.info("Recording walkover for match {} with winner {}", matchId, request.winnerRegistrationId());

        final var match = getMatchById(matchId);
        final var winner = registrationRepository
                .findById(request.winnerRegistrationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Registration not found with id: " + request.winnerRegistrationId()));

        if (!winner.equals(match.getTeam1()) && !winner.equals(match.getTeam2())) {
            throw new IllegalArgumentException("Winner must be one of the teams in the match");
        }

        match.walkover(winner);
        matchRepository.save(match);

        // Advance winner to next round
        TournamentAlgorithm algorithm =
                getTournamentAlgorithm(match.getTournament().getTournamentType());
        List<TournamentMatch> updatedMatches = algorithm.advanceWinner(match);
        matchRepository.saveAll(updatedMatches);

        return match;
    }

    public TournamentMatch recordMatchScore(Long matchId, Integer team1Score, Integer team2Score) {
        log.info("Recording score for match {}: {} - {}", matchId, team1Score, team2Score);

        final var match = getMatchById(matchId);

        if (!match.canStart()) {
            throw new IllegalStateException("Match is not ready to have score recorded");
        }

        if (team1Score.equals(team2Score)) {
            throw new IllegalArgumentException("Tournament matches cannot end in a tie");
        }

        // Get the teams
        final var team1 = match.getTeam1();
        final var team2 = match.getTeam2();

        // Create a game from the match
        final var game = createGameFromMatch(team1, team2, team1Score, team2Score);
        gameRepository.save(game);

        log.info("Created game {} for match {}", game.getId(), matchId);

        // Complete the match with the game
        return completeMatch(matchId, game.getId());
    }

    private Game createGameFromMatch(
            TournamentRegistration team1, TournamentRegistration team2, Integer team1Score, Integer team2Score) {

        // For singles, duplicate the player in both slots
        final var whitePlayer1 = team1.getPlayer();
        final var whitePlayer2 = isNull(team1.getPartner()) ? team1.getPlayer() : team1.getPartner();

        final var blackPlayer1 = team2.getPlayer();
        final var blackPlayer2 = isNull(team2.getPartner()) ? team2.getPlayer() : team2.getPartner();

        final var game = new Game(whitePlayer1, whitePlayer2, blackPlayer1, blackPlayer2);

        // Determine winner from scores (scores no longer stored)
        if (team1Score > team2Score) {
            game.setWinner(Game.TeamColor.WHITE);
        } else if (team2Score > team1Score) {
            game.setWinner(Game.TeamColor.BLACK);
        } else {
            throw new IllegalArgumentException("Tournament matches cannot end in a draw");
        }

        return game;
    }

    // Standings Management
    private void updateStandingsForMatch(TournamentMatch match) {
        if (!match.isCompleted()) {
            log.debug("Match {} is not completed or has no game, skipping standings update", match.getId());
            return;
        }

        final var tournament = match.getTournament();
        final var team1 = match.getTeam1();
        final var team2 = match.getTeam2();

        log.info("Updating standings for match {} in tournament {}", match.getId(), tournament.getId());

        // Get or create standings for both teams
        final var team1Standing = standingRepository
                .findByTournamentIdAndRegistrationId(tournament.getId(), team1.getId())
                .orElseGet(() -> {
                    final var standing = new TournamentStanding(tournament, team1);
                    return standingRepository.save(standing);
                });

        final var team2Standing = standingRepository
                .findByTournamentIdAndRegistrationId(tournament.getId(), team2.getId())
                .orElseGet(() -> {
                    final var standing = new TournamentStanding(tournament, team2);
                    return standingRepository.save(standing);
                });

        // Update standings based on match result
        team1Standing.recordMatch(match);
        team2Standing.recordMatch(match);

        // Save updated standings
        standingRepository.save(team1Standing);
        standingRepository.save(team2Standing);

        // Recalculate positions for all standings in the tournament
        recalculateStandingPositions(tournament.getId());

        log.info("Successfully updated standings for match {}", match.getId());
    }

    private void recalculateStandingPositions(Long tournamentId) {
        final var standings = standingRepository.findByTournamentIdOrderByPointsDesc(tournamentId);

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
        }

        standingRepository.saveAll(standings);
    }
}
