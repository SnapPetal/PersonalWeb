package solutions.thonbecker.personal.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;

@Service
@Slf4j
public class FoosballService {

    @Value("${foosball.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public FoosballService(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<FoosballPlayer> getAllPlayers() {
        try {
            final var players = restTemplate.getForObject(baseUrl + "/api/foosball/players", FoosballPlayer[].class);
            if (players != null) {
                for (final var player : players) {
                    // Fetch detailed player info to get the email
                    final var detailedPlayer = restTemplate.getForObject(
                            baseUrl + "/api/foosball/players/" + player.getId(), FoosballPlayer.class);
                    if (detailedPlayer != null) {
                        player.setEmail(detailedPlayer.getEmail());
                    }
                }
                return Arrays.asList(players);
            }
            return List.of();
        } catch (final ResourceAccessException e) {
            // Return empty list if service is unavailable
            return List.of();
        } catch (final RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball players response: {}", e.getMessage());
            return List.of();
        }
    }

    public FoosballPlayer createPlayer(final FoosballPlayer player) {
        try {
            return restTemplate.postForObject(baseUrl + "/api/foosball/players", player, FoosballPlayer.class);
        } catch (final ResourceAccessException e) {
            return null;
        } catch (final RestClientException e) {
            log.warn("Error creating foosball player: {}", e.getMessage());
            return null;
        }
    }

    public List<FoosballGame> getAllGames() {
        try {
            // Get all players first
            final var players = getAllPlayers();
            final Map<Long, FoosballGame> gamesMap = new HashMap<>();
            final Map<Long, String> playerNames = new HashMap<>();

            // Build player names map
            for (final var player : players) {
                playerNames.put(player.getId(), player.getName());
            }

            // Collect games from all players and build complete game objects
            for (final var player : players) {
                // Get detailed player info with games
                final var detailedPlayer = restTemplate.getForObject(
                        baseUrl + "/api/foosball/players/" + player.getId(), FoosballPlayer.class);

                if (detailedPlayer != null) {
                    // Process games from all positions
                    processPlayerGames(
                            detailedPlayer,
                            detailedPlayer.getWhiteTeamPlayer1Games(),
                            gamesMap,
                            playerNames,
                            "whiteTeamPlayer1");
                    processPlayerGames(
                            detailedPlayer,
                            detailedPlayer.getWhiteTeamPlayer2Games(),
                            gamesMap,
                            playerNames,
                            "whiteTeamPlayer2");
                    processPlayerGames(
                            detailedPlayer,
                            detailedPlayer.getBlackTeamPlayer1Games(),
                            gamesMap,
                            playerNames,
                            "blackTeamPlayer1");
                    processPlayerGames(
                            detailedPlayer,
                            detailedPlayer.getBlackTeamPlayer2Games(),
                            gamesMap,
                            playerNames,
                            "blackTeamPlayer2");
                }
            }

            // Convert to list and sort by ID (most recent first)
            return gamesMap.values().stream()
                    .sorted((g1, g2) -> Long.compare(g2.getId(), g1.getId()))
                    .collect(Collectors.toList());

        } catch (final ResourceAccessException e) {
            return List.of();
        } catch (final RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball games response: {}", e.getMessage());
            return List.of();
        }
    }

    private void processPlayerGames(
            final FoosballPlayer player,
            final List<FoosballGame> games,
            final Map<Long, FoosballGame> gamesMap,
            final Map<Long, String> playerNames,
            final String position) {
        if (games != null) {
            for (final var game : games) {
                var existingGame = gamesMap.get(game.getId());
                if (existingGame == null) {
                    // Create new game with player names
                    existingGame = new FoosballGame();
                    existingGame.setId(game.getId());
                    existingGame.setWhiteTeamScore(game.getWhiteTeamScore());
                    existingGame.setBlackTeamScore(game.getBlackTeamScore());
                    existingGame.setWhiteTeamGoalieScore(game.getWhiteTeamGoalieScore());
                    existingGame.setBlackTeamGoalieScore(game.getBlackTeamGoalieScore());
                    existingGame.setWhiteTeamForwardScore(game.getWhiteTeamForwardScore());
                    existingGame.setBlackTeamForwardScore(game.getBlackTeamForwardScore());
                    existingGame.setGameDate(game.getGameDate());
                    existingGame.setPlayedAt(game.getPlayedAt());
                    existingGame.setNotes(game.getNotes());
                    existingGame.setWinner(game.getWinner());
                    gamesMap.put(game.getId(), existingGame);
                }

                // Set player name based on position
                final var playerName = playerNames.get(player.getId());
                switch (position) {
                    case "whiteTeamPlayer1":
                        existingGame.setWhiteTeamPlayer1(playerName);
                        break;
                    case "whiteTeamPlayer2":
                        existingGame.setWhiteTeamPlayer2(playerName);
                        break;
                    case "blackTeamPlayer1":
                        existingGame.setBlackTeamPlayer1(playerName);
                        break;
                    case "blackTeamPlayer2":
                        existingGame.setBlackTeamPlayer2(playerName);
                        break;
                }
            }
        }
    }

    public FoosballGame createGame(final FoosballGame game) {
        try {
            return restTemplate.postForObject(baseUrl + "/api/foosball/games", game, FoosballGame.class);
        } catch (final ResourceAccessException e) {
            return null;
        } catch (final RestClientException e) {
            log.warn("Error creating foosball game: {}", e.getMessage());
            return null;
        }
    }

    public List<FoosballStats> getPlayerStats() {
        try {
            // Use the correct API endpoint
            final var stats =
                    restTemplate.getForObject(baseUrl + "/api/foosball/stats/players/all", FoosballStats[].class);
            if (stats != null) {
                // Sort by rank calculation: (games played * games played) / wins
                return Arrays.stream(stats)
                        .sorted((s1, s2) -> {
                            return s1.getPlayerName().compareTo(s2.getPlayerName());
                        })
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (final RestClientException e) {
            log.warn("Error fetching player stats from API: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean isServiceAvailable() {
        try {
            restTemplate.getForObject(baseUrl + "/actuator/health", Object.class);
            return true;
        } catch (final ResourceAccessException e) {
            return false;
        } catch (final RestClientException e) {
            log.warn("Error checking foosball service health: {}", e.getMessage());
            return false;
        }
    }
}
