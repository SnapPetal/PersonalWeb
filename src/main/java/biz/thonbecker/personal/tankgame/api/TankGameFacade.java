package biz.thonbecker.personal.tankgame.api;

import biz.thonbecker.personal.tankgame.domain.GameState;
import java.util.Map;

/**
 * Public API for the Tank Game module.
 * This facade defines the contract for interacting with the tank game system.
 */
public interface TankGameFacade {

    /**
     * Creates a new tank game instance.
     *
     * @return the created game state
     */
    GameState createGame();

    /**
     * Retrieves a game by its ID.
     *
     * @param gameId the game identifier
     * @return the game state, or null if not found
     */
    GameState getGame(String gameId);

    /**
     * Retrieves all active games.
     *
     * @return map of game IDs to game states
     */
    Map<String, GameState> getActiveGames();
}
