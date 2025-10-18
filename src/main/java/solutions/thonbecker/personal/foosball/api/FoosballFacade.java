package solutions.thonbecker.personal.foosball.api;

import solutions.thonbecker.personal.foosball.domain.Game;
import solutions.thonbecker.personal.foosball.domain.Player;
import solutions.thonbecker.personal.foosball.domain.PlayerStats;
import solutions.thonbecker.personal.foosball.domain.TeamStats;

import java.util.List;

/**
 * Public API for the Foosball module.
 * This is the only interface that other modules should use to interact with Foosball functionality.
 * All implementation details are hidden in the infrastructure package.
 */
public interface FoosballFacade {

    /**
     * Retrieves all registered players.
     *
     * @return List of all players
     */
    List<Player> getAllPlayers();

    /**
     * Creates a new player.
     *
     * @param player Player to create
     * @throws IllegalArgumentException if player is invalid
     */
    void createPlayer(Player player);

    /**
     * Retrieves statistics for all teams.
     *
     * @return List of team statistics
     */
    List<TeamStats> getTeamStats();

    /**
     * Creates a new game with the given details.
     *
     * @param game Game to create
     * @return The created game with assigned ID
     * @throws IllegalArgumentException if game is invalid
     */
    Game createGame(Game game);

    /**
     * Retrieves statistics for all players.
     *
     * @return List of player statistics
     */
    List<PlayerStats> getPlayerStats();

    /**
     * Retrieves recent games, ordered by most recent first.
     *
     * @return List of recent games
     */
    List<Game> getRecentGames();

    /**
     * Checks if the Foosball service is available.
     * Useful for health checks and graceful degradation.
     *
     * @return true if service is available, false otherwise
     */
    boolean isServiceAvailable();
}
