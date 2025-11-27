package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.api.FoosballFacade;
import biz.thonbecker.personal.foosball.domain.Game;
import biz.thonbecker.personal.foosball.domain.Player;
import biz.thonbecker.personal.foosball.domain.PlayerStats;
import biz.thonbecker.personal.foosball.domain.Team;
import biz.thonbecker.personal.foosball.domain.TeamStats;
import biz.thonbecker.personal.foosball.infrastructure.persistence.GameWithPlayers;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Foosball facade.
 * This class is package-private to enforce module boundaries.
 * External modules should only use the FoosballFacade interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class FoosballFacadeImpl implements FoosballFacade {

    private final FoosballService foosballService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<Player> getAllPlayers() {
        log.debug("Retrieving all players");
        return foosballService.getAllPlayers().stream()
                .map(this::toPlayerDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (player.getName() == null || player.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }

        log.info("Creating player: {}", player.getName());
        var createdPlayer = foosballService.createPlayer(player.getName());

        // Publish event
        eventPublisher.publishEvent(new biz.thonbecker.personal.foosball.api.PlayerCreatedEvent(
                createdPlayer.getId().toString(), createdPlayer.getName(), Instant.now()));
    }

    @Override
    public List<TeamStats> getTeamStats() {
        log.debug("Retrieving team statistics");
        return foosballService.getAllTeamStatsOrderedByWinPercentage().stream()
                .map(this::toTeamStatsDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Game createGame(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null");
        }
        if (game.getWhiteTeam() == null || game.getBlackTeam() == null) {
            throw new IllegalArgumentException("Both teams must be specified");
        }

        log.info(
                "Creating game: {} vs {}",
                game.getWhiteTeam().getPlayer1() + "&" + game.getWhiteTeam().getPlayer2(),
                game.getBlackTeam().getPlayer1() + "&" + game.getBlackTeam().getPlayer2());

        // Look up or create players
        var whitePlayer1 = foosballService
                .findPlayerByName(game.getWhiteTeam().getPlayer1())
                .orElseGet(
                        () -> foosballService.createPlayer(game.getWhiteTeam().getPlayer1()));
        var whitePlayer2 = foosballService
                .findPlayerByName(game.getWhiteTeam().getPlayer2())
                .orElseGet(
                        () -> foosballService.createPlayer(game.getWhiteTeam().getPlayer2()));
        var blackPlayer1 = foosballService
                .findPlayerByName(game.getBlackTeam().getPlayer1())
                .orElseGet(
                        () -> foosballService.createPlayer(game.getBlackTeam().getPlayer1()));
        var blackPlayer2 = foosballService
                .findPlayerByName(game.getBlackTeam().getPlayer2())
                .orElseGet(
                        () -> foosballService.createPlayer(game.getBlackTeam().getPlayer2()));

        // Convert GameResult to TeamColor
        var winner = game.getResult() != null
                ? switch (game.getResult()) {
                    case WHITE_TEAM_WIN ->
                        biz.thonbecker.personal.foosball.infrastructure.persistence.Game.TeamColor.WHITE;
                    case BLACK_TEAM_WIN ->
                        biz.thonbecker.personal.foosball.infrastructure.persistence.Game.TeamColor.BLACK;
                }
                : null;

        var createdGame = foosballService.recordGame(whitePlayer1, whitePlayer2, blackPlayer1, blackPlayer2, winner);

        var gameDomain = toGameDomainFromEntity(createdGame, game.getWhiteTeam(), game.getBlackTeam());

        // Publish event
        String winnerTeamName = determineWinnerTeamName(gameDomain);
        eventPublisher.publishEvent(new biz.thonbecker.personal.foosball.api.GameRecordedEvent(
                gameDomain.getId(),
                gameDomain.getWhiteTeam().getPlayer1() + " & "
                        + gameDomain.getWhiteTeam().getPlayer2(),
                0, // Scores not tracked anymore
                gameDomain.getBlackTeam().getPlayer1() + " & "
                        + gameDomain.getBlackTeam().getPlayer2(),
                0, // Scores not tracked anymore
                gameDomain.getResult(),
                winnerTeamName,
                Instant.now()));

        return gameDomain;
    }

    private String determineWinnerTeamName(Game game) {
        return switch (game.getResult()) {
            case WHITE_TEAM_WIN ->
                game.getWhiteTeam().getPlayer1() + " & " + game.getWhiteTeam().getPlayer2();
            case BLACK_TEAM_WIN ->
                game.getBlackTeam().getPlayer1() + " & " + game.getBlackTeam().getPlayer2();
        };
    }

    @Override
    public List<PlayerStats> getPlayerStats() {
        log.debug("Retrieving player statistics");
        return foosballService.getAllPlayerStatsOrderedByRankScore().stream()
                .map(this::toPlayerStatsDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Game> getRecentGames() {
        log.debug("Retrieving recent games");
        return foosballService.getRecentGames().stream().map(this::toGameDomain).collect(Collectors.toList());
    }

    @Override
    public boolean isServiceAvailable() {
        return true; // Local service is always available
    }

    // Mapper methods
    private Player toPlayerDomain(biz.thonbecker.personal.foosball.infrastructure.persistence.Player entity) {
        return new Player(entity.getId().toString(), entity.getName());
    }

    private PlayerStats toPlayerStatsDomain(
            biz.thonbecker.personal.foosball.infrastructure.persistence.PlayerStats projection) {
        return new PlayerStats(
                projection.getName(),
                projection.getTotalGames().intValue(),
                projection.getWins().intValue(),
                projection.getTotalGames().intValue() - projection.getWins().intValue(),
                0, // draws not tracked
                0, // goals scored not tracked
                0); // goals against not tracked
    }

    private TeamStats toTeamStatsDomain(
            biz.thonbecker.personal.foosball.infrastructure.persistence.TeamStats projection) {
        return new TeamStats(
                projection.getPlayer1Name(),
                projection.getPlayer2Name(),
                projection.getGamesPlayedTogether().intValue(),
                projection.getWins().intValue(),
                projection.getGamesPlayedTogether().intValue()
                        - projection.getWins().intValue(),
                0, // draws not tracked
                0, // goals scored not tracked
                0); // goals against not tracked
    }

    private Game toGameDomain(GameWithPlayers gameWithPlayers) {
        Team whiteTeam = new Team(gameWithPlayers.getWhiteTeamPlayer1Name(), gameWithPlayers.getWhiteTeamPlayer2Name());
        Team blackTeam = new Team(gameWithPlayers.getBlackTeamPlayer1Name(), gameWithPlayers.getBlackTeamPlayer2Name());

        var result = convertTeamColorToGameResult(gameWithPlayers.getWinner());
        var gameDomain = new Game(whiteTeam, blackTeam, result);
        gameDomain.setId(gameWithPlayers.getId());
        gameDomain.setPlayedAt(gameWithPlayers.getPlayedAt());

        return gameDomain;
    }

    private Game toGameDomainFromEntity(
            biz.thonbecker.personal.foosball.infrastructure.persistence.Game entity, Team whiteTeam, Team blackTeam) {
        var result = convertTeamColorToGameResult(entity.getWinner());
        var gameDomain = new Game(whiteTeam, blackTeam, result);
        gameDomain.setId(entity.getId());
        gameDomain.setPlayedAt(entity.getPlayedAt());

        return gameDomain;
    }

    private biz.thonbecker.personal.foosball.domain.GameResult convertTeamColorToGameResult(
            biz.thonbecker.personal.foosball.infrastructure.persistence.Game.TeamColor winner) {
        return switch (winner) {
            case WHITE -> biz.thonbecker.personal.foosball.domain.GameResult.WHITE_TEAM_WIN;
            case BLACK -> biz.thonbecker.personal.foosball.domain.GameResult.BLACK_TEAM_WIN;
        };
    }
}
