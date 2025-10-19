package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.domain.*;

import feign.FeignException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter that bridges the old FoosballApiClient (Feign) with the new module architecture.
 * This allows us to gradually migrate while maintaining backward compatibility.
 * Package-private to enforce module boundaries.
 */
@Component
@Slf4j
class FoosballApiClientAdapter implements FoosballClient {

    private final FoosballApiClient apiClient;

    FoosballApiClientAdapter(FoosballApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<Player> getAllPlayers() {
        try {
            return apiClient.getAllPlayers().stream()
                    .map(this::toPlayer)
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            log.error("Failed to retrieve players from API", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void createPlayer(Player player) {
        try {
            FoosballPlayer apiPlayer = toApiPlayer(player);
            apiClient.createPlayer(apiPlayer);
        } catch (FeignException e) {
            log.error("Failed to create player via API", e);
            throw new RuntimeException("Failed to create player", e);
        }
    }

    @Override
    public List<TeamStats> getTeamStats() {
        try {
            return apiClient.getTeamStats().stream()
                    .map(this::toTeamStats)
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            log.error("Failed to retrieve team stats from API", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Game createGame(Game game) {
        try {
            FoosballGame apiGame = toApiGame(game);
            FoosballGame result = apiClient.createGame(apiGame);
            // Only extract the ID from the response, keep the original game data
            if (result != null && result.getId() != null) {
                game.setId(result.getId());
            }
            return game;
        } catch (FeignException e) {
            log.error("Failed to create game via API", e);
            throw new RuntimeException("Failed to create game", e);
        }
    }

    @Override
    public List<PlayerStats> getPlayerStats() {
        try {
            return apiClient.getPlayerStats().stream()
                    .map(this::toPlayerStats)
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            log.error("Failed to retrieve player stats from API", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Game> getRecentGames() {
        try {
            return apiClient.getRecentGames().stream()
                    .map(this::toGame)
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            log.error("Failed to retrieve recent games from API", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            apiClient.checkHealth();
            return true;
        } catch (Exception e) {
            log.warn("Foosball API is not available", e);
            return false;
        }
    }

    // Conversion methods from API types to domain types

    private Player toPlayer(FoosballPlayer apiPlayer) {
        // Convert Long id to String
        String id = apiPlayer.getId() != null ? apiPlayer.getId().toString() : null;
        return new Player(id, apiPlayer.getName());
    }

    private FoosballPlayer toApiPlayer(Player player) {
        FoosballPlayer apiPlayer = new FoosballPlayer();
        // Convert String id to Long
        if (player.getId() != null) {
            try {
                apiPlayer.setId(Long.parseLong(player.getId()));
            } catch (NumberFormatException e) {
                log.warn("Invalid player ID format: {}", player.getId());
            }
        }
        apiPlayer.setName(player.getName());
        return apiPlayer;
    }

    private TeamStats toTeamStats(FoosballTeamStats apiStats) {
        return new TeamStats(
                apiStats.getPlayer1Name(),
                apiStats.getPlayer2Name(),
                apiStats.getGamesPlayedTogether(),
                apiStats.getWins(),
                apiStats.getLosses(),
                0, // draws not provided by API
                (int) apiStats.getAverageTeamScore(), // approximate goals scored
                0); // goals against not provided by API
    }

    private PlayerStats toPlayerStats(FoosballStats apiStats) {
        int draws = (apiStats.getGamesPlayed() != null
                        && apiStats.getWins() != null
                        && apiStats.getLosses() != null)
                ? apiStats.getGamesPlayed() - apiStats.getWins() - apiStats.getLosses()
                : 0;

        return new PlayerStats(
                apiStats.getPlayerName(),
                apiStats.getGamesPlayed() != null ? apiStats.getGamesPlayed() : 0,
                apiStats.getWins() != null ? apiStats.getWins() : 0,
                apiStats.getLosses() != null ? apiStats.getLosses() : 0,
                draws,
                apiStats.getGoalsScored() != null ? apiStats.getGoalsScored() : 0,
                apiStats.getGoalsConceded() != null ? apiStats.getGoalsConceded() : 0);
    }

    private Game toGame(FoosballGame apiGame) {
        Team whiteTeam = new Team(apiGame.getWhiteTeamPlayer1(), apiGame.getWhiteTeamPlayer2());
        Team blackTeam = new Team(apiGame.getBlackTeamPlayer1(), apiGame.getBlackTeamPlayer2());

        Game game = new Game(
                whiteTeam,
                blackTeam,
                apiGame.getWhiteTeamScore() != null ? apiGame.getWhiteTeamScore() : 0,
                apiGame.getBlackTeamScore() != null ? apiGame.getBlackTeamScore() : 0);
        game.setId(apiGame.getId());

        // Map playedAt field from API
        if (apiGame.getPlayedAt() != null && !apiGame.getPlayedAt().isEmpty()) {
            try {
                game.setPlayedAt(java.time.LocalDateTime.parse(apiGame.getPlayedAt()));
            } catch (Exception e) {
                log.warn("Failed to parse playedAt: {}", apiGame.getPlayedAt(), e);
            }
        }

        return game;
    }

    private FoosballGame toApiGame(Game game) {
        FoosballGame apiGame = new FoosballGame();
        apiGame.setId(game.getId());
        apiGame.setWhiteTeamPlayer1(game.getWhiteTeam().getPlayer1());
        apiGame.setWhiteTeamPlayer2(game.getWhiteTeam().getPlayer2());
        apiGame.setBlackTeamPlayer1(game.getBlackTeam().getPlayer1());
        apiGame.setBlackTeamPlayer2(game.getBlackTeam().getPlayer2());
        apiGame.setWhiteTeamScore(game.getWhiteTeamScore());
        apiGame.setBlackTeamScore(game.getBlackTeamScore());
        apiGame.setWinner(game.getWinner());
        apiGame.setDraw(game.isDraw());
        apiGame.setWhiteTeamWinner(game.isWhiteTeamWinner());
        apiGame.setBlackTeamWinner(game.isBlackTeamWinner());
        return apiGame;
    }
}
