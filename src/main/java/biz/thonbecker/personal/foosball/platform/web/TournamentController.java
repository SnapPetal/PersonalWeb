package biz.thonbecker.personal.foosball.platform.web;

import biz.thonbecker.personal.foosball.platform.TournamentService;
import biz.thonbecker.personal.foosball.platform.web.model.BracketViewDto;
import biz.thonbecker.personal.foosball.platform.web.model.CreateTournamentRequest;
import biz.thonbecker.personal.foosball.platform.web.model.MatchScoreRequest;
import biz.thonbecker.personal.foosball.platform.web.model.TournamentMatchResponse;
import biz.thonbecker.personal.foosball.platform.web.model.TournamentRegistrationRequest;
import biz.thonbecker.personal.foosball.platform.web.model.TournamentRegistrationResponse;
import biz.thonbecker.personal.foosball.platform.web.model.TournamentResponse;
import biz.thonbecker.personal.foosball.platform.web.model.TournamentSummaryDto;
import biz.thonbecker.personal.foosball.platform.web.model.UpdateTournamentRequest;
import biz.thonbecker.personal.foosball.platform.web.model.WalkoverRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    // Tournament CRUD Operations
    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody CreateTournamentRequest request, @RequestParam Long createdById) {
        log.info("Creating tournament: {} by player: {}", request.name(), createdById);

        final var tournament = tournamentService.createTournament(request, createdById);
        final var response = TournamentResponse.fromEntity(tournament);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TournamentSummaryDto>> getAllTournaments(Pageable pageable) {
        final var tournaments = tournamentService.getTournamentSummaries(pageable);
        return ResponseEntity.ok(tournaments.getContent());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TournamentResponse>> getActiveTournaments() {
        final var tournaments = tournamentService.getActiveTournaments();
        final var responses =
                tournaments.stream().map(TournamentResponse::fromEntitySummary).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<TournamentResponse>> getTournamentsForPlayer(@PathVariable Long playerId) {
        final var tournaments = tournamentService.getTournamentsForPlayer(playerId);
        final var responses =
                tournaments.stream().map(TournamentResponse::fromEntitySummary).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        final var tournament = tournamentService.getTournamentWithRegistrations(id);
        final var response = TournamentResponse.fromEntity(tournament);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentResponse> updateTournament(
            @PathVariable Long id, @Valid @RequestBody UpdateTournamentRequest request) {
        log.info("Updating tournament: {}", id);

        final var tournament = tournamentService.updateTournament(id, request);
        final var response = TournamentResponse.fromEntity(tournament);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        log.info("Deleting tournament: {}", id);
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    // Tournament Status Management
    @PostMapping("/{id}/registration/open")
    public ResponseEntity<TournamentResponse> openRegistration(@PathVariable Long id) {
        log.info("Opening registration for tournament: {}", id);

        final var tournament = tournamentService.openRegistration(id);
        final var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/registration/close")
    public ResponseEntity<TournamentResponse> closeRegistration(@PathVariable Long id) {
        log.info("Closing registration for tournament: {}", id);

        final var tournament = tournamentService.closeRegistration(id);
        final var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<TournamentResponse> startTournament(@PathVariable Long id) {
        log.info("Starting tournament: {}", id);

        final var tournament = tournamentService.startTournament(id);
        final var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TournamentResponse> cancelTournament(@PathVariable Long id) {
        log.info("Cancelling tournament: {}", id);

        final var tournament = tournamentService.cancelTournament(id);
        final var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    // Registration Management
    @PostMapping("/{id}/register")
    public ResponseEntity<TournamentRegistrationResponse> registerForTournament(
            @PathVariable Long id, @Valid @RequestBody TournamentRegistrationRequest request) {
        log.info("Registering player {} for tournament {}", request.playerId(), id);

        final var registration = tournamentService.registerForTournament(id, request);
        final var response = TournamentRegistrationResponse.fromEntity(registration);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/register/{playerId}")
    public ResponseEntity<Void> withdrawFromTournament(@PathVariable Long id, @PathVariable Long playerId) {
        log.info("Withdrawing player {} from tournament {}", playerId, id);

        tournamentService.withdrawFromTournament(id, playerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<List<TournamentRegistrationResponse>> getTournamentRegistrations(@PathVariable Long id) {
        final var registrations = tournamentService.getTournamentRegistrations(id);
        final var responses = registrations.stream()
                .map(TournamentRegistrationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // Bracket and Match Management
    @GetMapping("/{id}/bracket")
    public ResponseEntity<List<BracketViewDto>> getBracket(@PathVariable Long id) {
        final var bracket = tournamentService.getBracketView(id);
        return ResponseEntity.ok(bracket);
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<TournamentMatchResponse>> getTournamentMatches(@PathVariable Long id) {
        final var matches = tournamentService.getTournamentMatches(id);
        final var responses =
                matches.stream().map(TournamentMatchResponse::fromEntity).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<TournamentMatchResponse> getMatchById(@PathVariable Long matchId) {
        final var match = tournamentService.getMatchById(matchId);
        final var response = TournamentMatchResponse.fromEntity(match);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{matchId}/complete")
    public ResponseEntity<TournamentMatchResponse> completeMatch(
            @PathVariable Long matchId, @RequestParam Long gameId) {
        log.info("Completing match {} with game {}", matchId, gameId);

        final var match = tournamentService.completeMatch(matchId, gameId);
        final var response = TournamentMatchResponse.fromEntity(match);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{matchId}/walkover")
    public ResponseEntity<TournamentMatchResponse> recordWalkover(
            @PathVariable Long matchId, @Valid @RequestBody WalkoverRequest request) {
        log.info("Recording walkover for match {} with winner {}", matchId, request.winnerRegistrationId());

        final var match = tournamentService.recordWalkover(matchId, request);
        final var response = TournamentMatchResponse.fromEntity(match);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{matchId}/score")
    public ResponseEntity<TournamentMatchResponse> recordScore(
            @PathVariable Long matchId, @Valid @RequestBody MatchScoreRequest request) {
        log.info("Recording score for match {}: {} - {}", matchId, request.team1Score(), request.team2Score());

        final var match = tournamentService.recordMatchScore(matchId, request.team1Score(), request.team2Score());
        final var response = TournamentMatchResponse.fromEntity(match);

        return ResponseEntity.ok(response);
    }
}
