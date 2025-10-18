/**
 * Foosball Module
 *
 * <p>This module manages foosball game tracking, player management, and statistics.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link biz.thonbecker.personal.foosball.api.FoosballFacade} - Main entry point
 *   <li>{@link solutions.thonbecker.personal.foosball.domain} - Domain models (Game, Player,
 *       Stats)
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * <ul>
 *   <li>{@link solutions.thonbecker.personal.foosball.infrastructure} - All implementation details
 *   <li>FoosballClient - Internal interface for external API communication
 *   <li>FoosballApiClientAdapter - Adapter to legacy Feign client
 * </ul>
 *
 * <h2>Module Architecture</h2>
 * <pre>
 * foosball/
 * ├── api/                    (public interface)
 * │   └── FoosballFacade
 * ├── domain/                 (public domain models)
 * │   ├── Game
 * │   ├── Player
 * │   ├── Team
 * │   ├── GameResult
 * │   ├── PlayerStats
 * │   └── TeamStats
 * └── infrastructure/         (internal - package-private)
 *     ├── FoosballFacadeImpl
 *     ├── FoosballClient
 *     └── FoosballApiClientAdapter
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Autowired
 * private FoosballFacade foosballFacade;
 *
 * // Create a player
 * Player player = new Player("John Doe");
 * foosballFacade.createPlayer(player);
 *
 * // Record a game
 * Team whiteTeam = new Team("Alice", "Bob");
 * Team blackTeam = new Team("Charlie", "Dave");
 * Game game = new Game(whiteTeam, blackTeam, 10, 8);
 * foosballFacade.createGame(game);
 *
 * // Get statistics
 * List<PlayerStats> stats = foosballFacade.getPlayerStats();
 * }</pre>
 *
 * <h2>Dependencies</h2>
 * <p>This module depends on:
 * <ul>
 *   <li>shared - Common infrastructure and configuration</li>
 *   <li>External Foosball API (via Feign client)</li>
 * </ul>
 *
 * <h2>Spring Modulith</h2>
 * <p>This package is designed as a Spring Modulith module with enforced boundaries.
 * Only classes in the 'api' and 'domain' packages are public.
 * All implementation details in 'infrastructure' are package-private.
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Foosball",
        allowedDependencies = {"shared"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.springframework.lang.NonNullApi
package biz.thonbecker.personal.foosball;
