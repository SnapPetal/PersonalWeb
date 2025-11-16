/**
 * Foosball Module
 *
 * <p>This module manages foosball game tracking, player management, tournament system, and
 * comprehensive statistics with database persistence.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link biz.thonbecker.personal.foosball.api.FoosballFacade} - Main entry point for other
 *       modules
 *   <li>{@link biz.thonbecker.personal.foosball.domain} - Domain models (Game, Player, Team,
 *       Stats)
 *   <li>{@link biz.thonbecker.personal.foosball.api.GameRecordedEvent} - Event published when
 *       games are recorded
 *   <li>{@link biz.thonbecker.personal.foosball.api.PlayerCreatedEvent} - Event published when
 *       players are created
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * <ul>
 *   <li>{@link biz.thonbecker.personal.foosball.infrastructure} - All implementation details
 *   <li>FoosballFacadeImpl - Implementation of the public facade
 *   <li>FoosballService - Core business logic for games and players
 *   <li>TournamentService - Tournament bracket generation and management
 *   <li>CleanupService - Data cleanup operations
 *   <li>persistence package - JPA entities, repositories, and database views
 *   <li>web package - REST controllers, HTMX endpoints, and request/response models
 * </ul>
 *
 * <h2>Module Architecture</h2>
 * <pre>
 * foosball/
 * ├── api/                              (public interface)
 * │   ├── FoosballFacade
 * │   ├── GameRecordedEvent
 * │   └── PlayerCreatedEvent
 * ├── domain/                           (public domain models)
 * │   ├── Game
 * │   ├── Player
 * │   ├── Team
 * │   ├── GameResult
 * │   ├── PlayerStats
 * │   └── TeamStats
 * └── infrastructure/                   (internal - package-private)
 *     ├── FoosballFacadeImpl
 *     ├── FoosballService
 *     ├── TournamentService
 *     ├── CleanupService
 *     ├── persistence/
 *     │   ├── Game, Player, TeamStats, PlayerStats (entities)
 *     │   ├── *Repository (JPA repositories)
 *     │   └── *View (database views)
 *     ├── web/
 *     │   ├── FoosballController (HTMX endpoints)
 *     │   ├── FoosballRestController (REST API)
 *     │   ├── TournamentController (tournament endpoints)
 *     │   └── model/ (request/response DTOs)
 *     ├── tournament/
 *     │   └── algorithm/ (tournament algorithms)
 *     └── config/
 *         └── DataLoader (sample data initialization)
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
 * <h2>Database Schema</h2>
 * <p>This module uses the 'foosball' schema in PostgreSQL with the following tables:
 * <ul>
 *   <li>players - Player information (name, email)</li>
 *   <li>games - Game records with team compositions and scores</li>
 *   <li>player_stats - Materialized view of player statistics</li>
 *   <li>team_stats - Materialized view of team performance</li>
 *   <li>tournaments - Tournament definitions</li>
 *   <li>tournament_registrations - Player registrations for tournaments</li>
 *   <li>tournament_matches - Tournament bracket matches</li>
 *   <li>tournament_standings - Final tournament standings</li>
 * </ul>
 * <p>Database migrations are managed by Liquibase in resources/db/changelog/foosball/
 *
 * <h2>Dependencies</h2>
 * <p>This module depends on:
 * <ul>
 *   <li>shared - Common infrastructure and configuration</li>
 *   <li>Spring Data JPA - Database access and repository support</li>
 *   <li>PostgreSQL - Relational database for persistence</li>
 *   <li>Liquibase - Database schema migration</li>
 * </ul>
 *
 * <h2>Spring Modulith</h2>
 * <p>This package is designed as a Spring Modulith module with enforced boundaries. Only classes
 * in the 'api' and 'domain' packages are public. All implementation details in 'infrastructure'
 * are package-private and cannot be accessed by other modules.
 *
 * <h2>Migration Notes</h2>
 * <p>This module was migrated from a separate microservice into the monolith as part of the Spring
 * Modulith architecture adoption. All external API dependencies have been replaced with direct
 * database access using JPA repositories.
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Foosball",
        allowedDependencies = {"shared"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.springframework.lang.NonNullApi
package biz.thonbecker.personal.foosball;
