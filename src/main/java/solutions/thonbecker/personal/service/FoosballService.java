package solutions.thonbecker.personal.service;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;
import solutions.thonbecker.personal.types.FoosballTeamStats;

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

    public List<FoosballTeamStats> getTeamStats() {
        try {
            final var stats =
                    restTemplate.getForObject(baseUrl + "/api/foosball/stats/teams/all", FoosballTeamStats[].class);
            if (stats != null) {
                return Arrays.asList(stats);
            }
            return List.of();
        } catch (final ResourceAccessException e) {
            return List.of();
        } catch (final RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball team stats response: {}", e.getMessage());
            return List.of();
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
            final var stats =
                    restTemplate.getForObject(baseUrl + "/api/foosball/stats/players/all", FoosballStats[].class);
            if (stats != null) {
                return Arrays.asList(stats);
            }
            return List.of();
        } catch (final RestClientException e) {
            log.warn("Error fetching player stats from API: {}", e.getMessage());
            return List.of();
        }
    }

    public List<FoosballGame> getRecentGames() {
        try {
            final var games = restTemplate.getForObject(baseUrl + "/api/foosball/games/recent", FoosballGame[].class);
            if (games != null) {
                return Arrays.asList(games);
            }
            return List.of();
        } catch (final ResourceAccessException e) {
            return List.of();
        } catch (final RestClientException e) {
            log.warn("Error fetching recent games from API: {}", e.getMessage());
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
