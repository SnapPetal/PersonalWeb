package solutions.thonbecker.personal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballStats;

import java.util.Arrays;
import java.util.List;

@Service
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
        }
    }
    
    public boolean isServiceAvailable() {
        try {
            restTemplate.getForObject(baseUrl + "/actuator/health", String.class);
            return true;
        } catch (ResourceAccessException e) {
            return false;
        }
    }
}
