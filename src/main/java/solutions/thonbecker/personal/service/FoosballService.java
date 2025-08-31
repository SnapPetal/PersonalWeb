package solutions.thonbecker.personal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballStats;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FoosballService {
    
    @Value("${foosball.api.base-url:http://localhost:8080}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    public FoosballService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<FoosballPlayer> getAllPlayers() {
        try {
            FoosballPlayer[] players = restTemplate.getForObject(
                baseUrl + "/api/foosball/players", 
                FoosballPlayer[].class
            );
            return players != null ? Arrays.asList(players) : List.of();
        } catch (ResourceAccessException e) {
            // Return empty list if service is unavailable
            return List.of();
        } catch (RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball players response: {}", e.getMessage());
            return List.of();
        }
    }
    
    public FoosballPlayer createPlayer(FoosballPlayer player) {
        try {
            return restTemplate.postForObject(
                baseUrl + "/api/foosball/players", 
                player, 
                FoosballPlayer.class
            );
        } catch (ResourceAccessException e) {
            return null;
        } catch (RestClientException e) {
            log.warn("Error creating foosball player: {}", e.getMessage());
            return null;
        }
    }
    
    public List<FoosballGame> getAllGames() {
        try {
            FoosballGame[] games = restTemplate.getForObject(
                baseUrl + "/api/foosball/games", 
                FoosballGame[].class
            );
            return games != null ? Arrays.asList(games) : List.of();
        } catch (ResourceAccessException e) {
            return List.of();
        } catch (RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball games response: {}", e.getMessage());
            return List.of();
        }
    }
    
    public FoosballGame createGame(FoosballGame game) {
        try {
            return restTemplate.postForObject(
                baseUrl + "/api/foosball/games", 
                game, 
                FoosballGame.class
            );
        } catch (ResourceAccessException e) {
            return null;
        } catch (RestClientException e) {
            log.warn("Error creating foosball game: {}", e.getMessage());
            return null;
        }
    }
    
    public List<FoosballStats> getPlayerStats() {
        try {
            FoosballStats[] stats = restTemplate.getForObject(
                baseUrl + "/api/foosball/stats/players", 
                FoosballStats[].class
            );
            return stats != null ? Arrays.asList(stats) : List.of();
        } catch (ResourceAccessException e) {
            return List.of();
        } catch (RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball player stats response: {}", e.getMessage());
            return List.of();
        }
    }
    
    public List<FoosballStats> getPositionStats() {
        try {
            FoosballStats[] stats = restTemplate.getForObject(
                baseUrl + "/api/foosball/stats/position", 
                FoosballStats[].class
            );
            return stats != null ? Arrays.asList(stats) : List.of();
        } catch (ResourceAccessException e) {
            return List.of();
        } catch (RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball position stats response: {}", e.getMessage());
            return List.of();
        }
    }
    
    public boolean isServiceAvailable() {
        try {
            restTemplate.getForObject(baseUrl + "/actuator/health", Object.class);
            return true;
        } catch (ResourceAccessException e) {
            return false;
        } catch (RestClientException e) {
            log.warn("Error checking foosball service health: {}", e.getMessage());
            return false;
        }
    }
}
