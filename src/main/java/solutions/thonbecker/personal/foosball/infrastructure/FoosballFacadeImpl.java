package solutions.thonbecker.personal.foosball.infrastructure;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import solutions.thonbecker.personal.foosball.api.FoosballFacade;
import solutions.thonbecker.personal.foosball.domain.Game;
import solutions.thonbecker.personal.foosball.domain.Player;
import solutions.thonbecker.personal.foosball.domain.PlayerStats;
import solutions.thonbecker.personal.foosball.domain.TeamStats;

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

    FoosballFacadeImpl(FoosballClient foosballClient) {
        this.foosballClient = foosballClient;
    }

    @Override
    public List<Player> getAllPlayers() {
        log.debug("Retrieving all players");
        return foosballClient.getAllPlayers();
    }

    @Override
    public void createPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (player.getName() == null || player.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }

        log.info("Creating player: {}", player.getName());
        foosballClient.createPlayer(player);
    }

    @Override
    public List<TeamStats> getTeamStats() {
        log.debug("Retrieving team statistics");
        return foosballClient.getTeamStats();
    }

    @Override
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

        return foosballClient.createGame(game);
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
