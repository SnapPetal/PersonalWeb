package solutions.thonbecker.personal.foosball.infrastructure;

import solutions.thonbecker.personal.foosball.domain.Game;
import solutions.thonbecker.personal.foosball.domain.Player;
import solutions.thonbecker.personal.foosball.domain.PlayerStats;
import solutions.thonbecker.personal.foosball.domain.TeamStats;

import java.util.List;

/**
 * Internal interface for Foosball external API communication.
 * Package-private to enforce module boundaries.
 */
interface FoosballClient {

    List<Player> getAllPlayers();

    void createPlayer(Player player);

    List<TeamStats> getTeamStats();

    Game createGame(Game game);

    List<PlayerStats> getPlayerStats();

    List<Game> getRecentGames();

    boolean isServiceAvailable();
}
