package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.domain.Game;
import biz.thonbecker.personal.foosball.domain.Player;
import biz.thonbecker.personal.foosball.domain.PlayerStats;
import biz.thonbecker.personal.foosball.domain.TeamStats;

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
