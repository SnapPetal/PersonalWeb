package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.api.FoosballFacade;
import biz.thonbecker.personal.foosball.domain.Game;
import biz.thonbecker.personal.foosball.domain.Player;
import biz.thonbecker.personal.foosball.domain.PlayerStats;
import biz.thonbecker.personal.foosball.domain.TeamStats;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of the Foosball facade.
 * This class is package-private to enforce module boundaries.
 * External modules should only use the FoosballFacade interface.
 */
@Service
@Slf4j
class FoosballFacadeImpl implements FoosballFacade {

    private final FoosballClient foosballClient;
    private final ApplicationEventPublisher eventPublisher;

    FoosballFacadeImpl(FoosballClient foosballClient, ApplicationEventPublisher eventPublisher) {
        this.foosballClient = foosballClient;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<Player> getAllPlayers() {
        log.debug("Retrieving all players");
        return foosballClient.getAllPlayers();
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
        foosballClient.createPlayer(player);

        // Publish event (note: player.getId() may be null before creation)
        if (player.getId() != null) {
            eventPublisher.publishEvent(new biz.thonbecker.personal.foosball.api.PlayerCreatedEvent(
                    null, // ID not available until after API call
                    player.getName(),
                    Instant.now()));
        }
    }

    @Override
    public List<TeamStats> getTeamStats() {
        log.debug("Retrieving team statistics");
        return foosballClient.getTeamStats();
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

        Game createdGame = foosballClient.createGame(game);

        // Publish event
        String winnerTeamName = determineWinnerTeamName(createdGame);
        eventPublisher.publishEvent(new biz.thonbecker.personal.foosball.api.GameRecordedEvent(
                createdGame.getId(),
                createdGame.getWhiteTeam().getPlayer1() + " & "
                        + createdGame.getWhiteTeam().getPlayer2(),
                createdGame.getWhiteTeamScore(),
                createdGame.getBlackTeam().getPlayer1() + " & "
                        + createdGame.getBlackTeam().getPlayer2(),
                createdGame.getBlackTeamScore(),
                createdGame.getResult(),
                winnerTeamName,
                Instant.now()));

        return createdGame;
    }

    private String determineWinnerTeamName(Game game) {
        return switch (game.getResult()) {
            case WHITE_TEAM_WIN ->
                game.getWhiteTeam().getPlayer1() + " & " + game.getWhiteTeam().getPlayer2();
            case BLACK_TEAM_WIN ->
                game.getBlackTeam().getPlayer1() + " & " + game.getBlackTeam().getPlayer2();
            case DRAW -> "Draw";
        };
    }

    @Override
    public List<PlayerStats> getPlayerStats() {
        log.debug("Retrieving player statistics");
        return foosballClient.getPlayerStats();
    }

    @Override
    public List<Game> getRecentGames() {
        log.debug("Retrieving recent games");
        return foosballClient.getRecentGames();
    }

    @Override
    public boolean isServiceAvailable() {
        return foosballClient.isServiceAvailable();
    }
}
