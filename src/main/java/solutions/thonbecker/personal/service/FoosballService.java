package solutions.thonbecker.personal.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;
import solutions.thonbecker.personal.types.FoosballTeamStats;

import java.util.List;

@Service
@Slf4j
public class FoosballService {

    private final FoosballApiClient foosballApiClient;

    public FoosballService(final FoosballApiClient foosballApiClient) {
        this.foosballApiClient = foosballApiClient;
    }

    public List<FoosballPlayer> getAllPlayers() {
        try {
            return foosballApiClient.getAllPlayers();
        } catch (final Exception e) {
            log.warn("Error fetching foosball players: {}", e.getMessage());
            return List.of();
        }
    }

    public FoosballPlayer createPlayer(final FoosballPlayer player) {
        try {
            return foosballApiClient.createPlayer(player);
        } catch (final Exception e) {
            log.warn("Error creating foosball player: {}", e.getMessage());
            throw new RuntimeException("Failed to create foosball player: " + e.getMessage(), e);
        }
    }

    public List<FoosballTeamStats> getTeamStats() {
        try {
            return foosballApiClient.getTeamStats();
        } catch (final Exception e) {
            log.warn("Error fetching foosball team stats: {}", e.getMessage());
            return List.of();
        }
    }

    public FoosballGame createGame(final FoosballGame game) {
        try {
            return foosballApiClient.createGame(game);
        } catch (final Exception e) {
            log.warn("Error creating foosball game: {}", e.getMessage());
            throw new RuntimeException("Failed to create foosball game: " + e.getMessage(), e);
        }
    }

    public List<FoosballStats> getPlayerStats() {
        try {
            return foosballApiClient.getPlayerStats();
        } catch (final Exception e) {
            log.warn("Error fetching player stats from API: {}", e.getMessage());
            return List.of();
        }
    }

    public List<FoosballGame> getRecentGames() {
        try {
            return foosballApiClient.getRecentGames();
        } catch (final Exception e) {
            log.warn("Error fetching recent games from API: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean isServiceAvailable() {
        try {
            foosballApiClient.checkHealth();
            return true;
        } catch (final Exception e) {
            log.warn("Error checking foosball service health: {}", e.getMessage());
            return false;
        }
    }
}
