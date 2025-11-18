package biz.thonbecker.personal.foosball.infrastructure.web;

import biz.thonbecker.personal.foosball.infrastructure.TournamentService;
import biz.thonbecker.personal.foosball.infrastructure.persistence.BracketView;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentSummary;
import biz.thonbecker.personal.foosball.infrastructure.web.model.*;
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
@CrossOrigin(
        origins = {"http://localhost:3000", "http://localhost:8080"},
        allowCredentials = "false")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    // Tournament CRUD Operations
    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody CreateTournamentRequest request, @RequestParam Long createdById) {
        log.info("Creating tournament: {} by player: {}", request.name(), createdById);

        var tournament = tournamentService.createTournament(request, createdById);
        var response = TournamentResponse.fromEntity(tournament);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TournamentSummary>> getAllTournaments(Pageable pageable) {
        var tournaments = tournamentService.getTournamentSummaries(pageable);
        return ResponseEntity.ok(tournaments.getContent());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TournamentResponse>> getActiveTournaments() {
        var tournaments = tournamentService.getActiveTournaments();
        var responses =
                tournaments.stream().map(TournamentResponse::fromEntitySummary).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<TournamentResponse>> getTournamentsForPlayer(@PathVariable Long playerId) {
        var tournaments = tournamentService.getTournamentsForPlayer(playerId);
        var responses =
                tournaments.stream().map(TournamentResponse::fromEntitySummary).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        var tournament = tournamentService.getTournamentWithRegistrations(id);
        var response = TournamentResponse.fromEntity(tournament);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentResponse> updateTournament(
            @PathVariable Long id, @Valid @RequestBody UpdateTournamentRequest request) {
        log.info("Updating tournament: {}", id);

        var tournament = tournamentService.updateTournament(id, request);
        var response = TournamentResponse.fromEntity(tournament);

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

        var tournament = tournamentService.openRegistration(id);
        var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/registration/close")
    public ResponseEntity<TournamentResponse> closeRegistration(@PathVariable Long id) {
        log.info("Closing registration for tournament: {}", id);

        var tournament = tournamentService.closeRegistration(id);
        var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<TournamentResponse> startTournament(@PathVariable Long id) {
        log.info("Starting tournament: {}", id);

        var tournament = tournamentService.startTournament(id);
        var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TournamentResponse> cancelTournament(@PathVariable Long id) {
        log.info("Cancelling tournament: {}", id);

        var tournament = tournamentService.cancelTournament(id);
        var response = TournamentResponse.fromEntitySummary(tournament);

        return ResponseEntity.ok(response);
    }

    // Registration Management
    @PostMapping("/{id}/register")
    public ResponseEntity<TournamentRegistrationResponse> registerForTournament(
            @PathVariable Long id, @Valid @RequestBody TournamentRegistrationRequest request) {
        log.info("Registering player {} for tournament {}", request.playerId(), id);

        var registration = tournamentService.registerForTournament(id, request);
        var response = TournamentRegistrationResponse.fromEntity(registration);

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
        var registrations = tournamentService.getTournamentRegistrations(id);
        var responses = registrations.stream()
                .map(TournamentRegistrationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // Bracket and Match Management
    @GetMapping("/{id}/bracket")
    public ResponseEntity<List<BracketView>> getBracket(@PathVariable Long id) {
        var bracket = tournamentService.getBracketView(id);
        return ResponseEntity.ok(bracket);
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<TournamentMatchResponse>> getTournamentMatches(@PathVariable Long id) {
        var matches = tournamentService.getTournamentMatches(id);
        var responses =
                matches.stream().map(TournamentMatchResponse::fromEntity).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<TournamentMatchResponse> getMatchById(@PathVariable Long matchId) {
        var match = tournamentService.getMatchById(matchId);
        var response = TournamentMatchResponse.fromEntity(match);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{matchId}/complete")
    public ResponseEntity<TournamentMatchResponse> completeMatch(
            @PathVariable Long matchId, @RequestParam Long gameId) {
        log.info("Completing match {} with game {}", matchId, gameId);

        var match = tournamentService.completeMatch(matchId, gameId);
        var response = TournamentMatchResponse.fromEntity(match);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{matchId}/walkover")
    public ResponseEntity<TournamentMatchResponse> recordWalkover(
            @PathVariable Long matchId, @Valid @RequestBody WalkoverRequest request) {
        log.info("Recording walkover for match {} with winner {}", matchId, request.winnerRegistrationId());

        var match = tournamentService.recordWalkover(matchId, request);
        var response = TournamentMatchResponse.fromEntity(match);

        return ResponseEntity.ok(response);
    }
}
