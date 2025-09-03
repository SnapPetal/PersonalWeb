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

    @Value("${foosball.api.base-url:http://localhost:8080}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public FoosballService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<FoosballPlayer> getAllPlayers() {
        try {
            FoosballPlayer[] players =
                    restTemplate.getForObject(baseUrl + "/api/foosball/players", FoosballPlayer[].class);
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
            return restTemplate.postForObject(baseUrl + "/api/foosball/players", player, FoosballPlayer.class);
        } catch (ResourceAccessException e) {
            return null;
        } catch (RestClientException e) {
            log.warn("Error creating foosball player: {}", e.getMessage());
            return null;
        }
    }

    public List<FoosballGame> getAllGames() {
        try {
            // Get all players first
            List<FoosballPlayer> players = getAllPlayers();
            Map<Long, FoosballGame> gamesMap = new HashMap<>();
            Map<Long, String> playerNames = new HashMap<>();
            
            // Build player names map
            for (FoosballPlayer player : players) {
                playerNames.put(player.getId(), player.getName());
            }
            
            // Collect games from all players and build complete game objects
            for (FoosballPlayer player : players) {
                // Get detailed player info with games
                FoosballPlayer detailedPlayer = restTemplate.getForObject(
                    baseUrl + "/api/foosball/players/" + player.getId(), 
                    FoosballPlayer.class
                );
                
                if (detailedPlayer != null) {
                    // Process games from all positions
                    processPlayerGames(detailedPlayer, detailedPlayer.getWhiteTeamPlayer1Games(), 
                        gamesMap, playerNames, "whiteTeamPlayer1");
                    processPlayerGames(detailedPlayer, detailedPlayer.getWhiteTeamPlayer2Games(), 
                        gamesMap, playerNames, "whiteTeamPlayer2");
                    processPlayerGames(detailedPlayer, detailedPlayer.getBlackTeamPlayer1Games(), 
                        gamesMap, playerNames, "blackTeamPlayer1");
                    processPlayerGames(detailedPlayer, detailedPlayer.getBlackTeamPlayer2Games(), 
                        gamesMap, playerNames, "blackTeamPlayer2");
                }
            }
            
            // Convert to list and sort by ID (most recent first)
            return gamesMap.values().stream()
                .sorted((g1, g2) -> Long.compare(g2.getId(), g1.getId()))
                .collect(Collectors.toList());
                
        } catch (ResourceAccessException e) {
            return List.of();
        } catch (RestClientException e) {
            // Return empty list if there are parsing errors
            log.warn("Error parsing foosball games response: {}", e.getMessage());
            return List.of();
        }
    }
    
    private void processPlayerGames(FoosballPlayer player, List<FoosballGame> games, 
                                  Map<Long, FoosballGame> gamesMap, Map<Long, String> playerNames,
                                  String position) {
        if (games != null) {
            for (FoosballGame game : games) {
                FoosballGame existingGame = gamesMap.get(game.getId());
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
                String playerName = playerNames.get(player.getId());
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

    public FoosballGame createGame(FoosballGame game) {
        try {
            return restTemplate.postForObject(baseUrl + "/api/foosball/games", game, FoosballGame.class);
        } catch (ResourceAccessException e) {
            return null;
        } catch (RestClientException e) {
            log.warn("Error creating foosball game: {}", e.getMessage());
            return null;
        }
    }

    public List<FoosballStats> getPlayerStats() {
        try {
            // Try the API first, but if it fails, calculate stats from games
            FoosballStats[] stats =
                    restTemplate.getForObject(baseUrl + "/api/foosball/stats/players", FoosballStats[].class);
            if (stats != null) {
                return Arrays.asList(stats);
            }
        } catch (RestClientException e) {
            log.debug("Stats API not available, calculating from games: {}", e.getMessage());
        }
        
        // Calculate stats from games if API is not available
        return calculatePlayerStatsFromGames();
    }


    
    private List<FoosballStats> calculatePlayerStatsFromGames() {
        List<FoosballPlayer> players = getAllPlayers();
        List<FoosballGame> games = getAllGames();
        Map<String, FoosballStats> playerStatsMap = new HashMap<>();
        
        // Initialize stats for all players
        for (FoosballPlayer player : players) {
            FoosballStats stats = new FoosballStats();
            stats.setPlayerName(player.getName());
            stats.setGamesPlayed(0);
            stats.setWins(0);
            stats.setWinPercentage(0.0);
            playerStatsMap.put(player.getName(), stats);
        }
        
        // Calculate stats from games
        for (FoosballGame game : games) {
            // Count games and wins for each player
            String[] players1 = {game.getWhiteTeamPlayer1(), game.getWhiteTeamPlayer2()};
            String[] players2 = {game.getBlackTeamPlayer1(), game.getBlackTeamPlayer2()};
            boolean team1Won = "WHITE".equals(game.getWinner());
            
            // Update stats for team 1 players
            for (String playerName : players1) {
                if (playerName != null && playerStatsMap.containsKey(playerName)) {
                    FoosballStats stats = playerStatsMap.get(playerName);
                    stats.setGamesPlayed(stats.getGamesPlayed() + 1);
                    if (team1Won) {
                        stats.setWins(stats.getWins() + 1);
                    }
                }
            }
            
            // Update stats for team 2 players
            for (String playerName : players2) {
                if (playerName != null && playerStatsMap.containsKey(playerName)) {
                    FoosballStats stats = playerStatsMap.get(playerName);
                    stats.setGamesPlayed(stats.getGamesPlayed() + 1);
                    if (!team1Won) {
                        stats.setWins(stats.getWins() + 1);
                    }
                }
            }
        }
        
        // Calculate win percentages
        for (FoosballStats stats : playerStatsMap.values()) {
            if (stats.getGamesPlayed() > 0) {
                double winPercentage = (double) stats.getWins() / stats.getGamesPlayed() * 100;
                stats.setWinPercentage(winPercentage);
            }
        }
        
        return playerStatsMap.values().stream()
                .filter(stats -> stats.getGamesPlayed() > 0)
                .sorted((s1, s2) -> Double.compare(s2.getWinPercentage(), s1.getWinPercentage()))
                .collect(Collectors.toList());
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
