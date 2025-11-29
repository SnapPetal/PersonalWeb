package biz.thonbecker.personal.foosball.infrastructure.config;

import biz.thonbecker.personal.foosball.infrastructure.FoosballService;
import biz.thonbecker.personal.foosball.infrastructure.persistence.Game;
import biz.thonbecker.personal.foosball.infrastructure.persistence.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    private final FoosballService foosballService;

    @Value("${foosball.sample-data.enabled:true}")
    private boolean sampleDataEnabled;

    /**
     * Helper method to get an existing player or create a new one
     */
    private Player getOrCreatePlayer(String name, String email, int rating, int gamesPlayed) {
        // First try to find existing player
        final var existingPlayer = foosballService.findPlayerByName(name);
        if (existingPlayer.isPresent()) {
            log.info("Player {} already exists, using existing player.", name);
            return existingPlayer.get();
        }

        // If player doesn't exist, create new one with specific rating
        log.info("Creating new player: {} with rating {}", name, rating);
        var player = foosballService.createPlayer(name, email);
        player.setRating(rating);
        player.setGamesPlayed(gamesPlayed);
        player.setPeakRating(rating);
        return player;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run(ApplicationReadyEvent event) throws Exception {
        if (!sampleDataEnabled) {
            return;
        }

        log.info("Loading sample foosball data...");

        // Create sample players with varied ratings across different rank tiers
        // Master tier (1800-1999)
        final var alice = getOrCreatePlayer("Alice", "alice@example.com", 1850, 10);

        // Diamond tier (1600-1799)
        final var bob = getOrCreatePlayer("Bob", "bob@example.com", 1650, 8);

        // Platinum tier (1400-1599)
        final var charlie = getOrCreatePlayer("Charlie", "charlie@example.com", 1450, 7);
        final var diana = getOrCreatePlayer("Diana", "diana@example.com", 1480, 6);

        // Gold tier (1200-1399)
        final var eve = getOrCreatePlayer("Eve", "eve@example.com", 1250, 8);
        final var frank = getOrCreatePlayer("Frank", "frank@example.com", 1280, 7);

        // Silver tier (1000-1199)
        final var grace = getOrCreatePlayer("Grace", "grace@example.com", 1100, 6);
        final var henry = getOrCreatePlayer("Henry", "henry@example.com", 1120, 5);

        // Bronze tier (0-999)
        final var iris = getOrCreatePlayer("Iris", "iris@example.com", 950, 6);
        final var jack = getOrCreatePlayer("Jack", "jack@example.com", 920, 5);

        // Players with fewer games (won't show in rankings with 5+ game filter)
        final var kate = getOrCreatePlayer("Kate", "kate@example.com", 1000, 3);
        final var liam = getOrCreatePlayer("Liam", "liam@example.com", 1000, 2);

        log.info("Created {} players", foosballService.getTotalPlayers());

        // Check if we already have games to avoid duplicates
        if (foosballService.getTotalGames() > 0) {
            log.info("Sample games already exist, skipping game creation.");
            return;
        }

        // Record varied sample games with different outcomes

        // Close competitive games
        foosballService.recordGame(alice, bob, charlie, diana, Game.TeamColor.WHITE);
        foosballService.recordGame(eve, frank, grace, henry, Game.TeamColor.WHITE);
        foosballService.recordGame(iris, jack, kate, liam, Game.TeamColor.BLACK);

        // Some dominant victories
        foosballService.recordGame(alice, charlie, bob, diana, Game.TeamColor.WHITE); // 5-1 -> WHITE wins
        foosballService.recordGame(grace, iris, eve, frank, Game.TeamColor.WHITE); // 5-0 -> WHITE wins

        // Mixed matchups
        foosballService.recordGame(bob, henry, alice, kate, Game.TeamColor.WHITE);
        foosballService.recordGame(charlie, jack, diana, liam, Game.TeamColor.WHITE);
        foosballService.recordGame(eve, kate, frank, iris, Game.TeamColor.BLACK);

        // Upset victories (weaker players beating stronger ones)
        foosballService.recordGame(liam, diana, alice, bob, Game.TeamColor.WHITE); // 5-4 -> WHITE wins
        foosballService.recordGame(frank, henry, charlie, grace, Game.TeamColor.WHITE); // 5-3 -> WHITE wins

        // High-scoring games
        foosballService.recordGame(alice, iris, eve, jack, Game.TeamColor.WHITE); // 8-6 -> WHITE wins
        foosballService.recordGame(bob, kate, henry, liam, Game.TeamColor.WHITE); // 7-5 -> WHITE wins

        // Low-scoring defensive games
        foosballService.recordGame(charlie, frank, diana, grace, Game.TeamColor.WHITE); // 3-1 -> WHITE wins
        foosballService.recordGame(alice, liam, bob, iris, Game.TeamColor.WHITE); // 2-1 -> WHITE wins

        // Close games for variety
        foosballService.recordGame(eve, henry, charlie, kate, Game.TeamColor.BLACK); // 4-5 -> BLACK wins
        foosballService.recordGame(frank, jack, diana, iris, Game.TeamColor.WHITE); // 6-5 -> WHITE wins

        // Tournament-style progression games
        foosballService.recordGame(alice, charlie, bob, diana, Game.TeamColor.WHITE); // 5-2 -> WHITE wins
        foosballService.recordGame(eve, grace, frank, henry, Game.TeamColor.WHITE); // 5-3 -> WHITE wins
        foosballService.recordGame(iris, kate, jack, liam, Game.TeamColor.WHITE); // 5-4 -> WHITE wins

        // Championship-style games
        foosballService.recordGame(alice, eve, charlie, grace, Game.TeamColor.WHITE); // 5-4 -> WHITE wins
        foosballService.recordGame(bob, iris, diana, kate, Game.TeamColor.WHITE); // 5-3 -> WHITE wins

        // Final championship
        foosballService.recordGame(alice, bob, eve, iris, Game.TeamColor.WHITE); // 6-5 -> WHITE wins

        log.info("Sample data loaded successfully!");
        log.info("Total players: {}", foosballService.getTotalPlayers());
        log.info("Total games: {}", foosballService.getTotalGames());
    }
}
