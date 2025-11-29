package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.infrastructure.persistence.Game;
import biz.thonbecker.personal.foosball.infrastructure.persistence.GameRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.GameWithPlayers;
import biz.thonbecker.personal.foosball.infrastructure.persistence.Player;
import biz.thonbecker.personal.foosball.infrastructure.persistence.PlayerRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.PlayerStats;
import biz.thonbecker.personal.foosball.infrastructure.persistence.PlayerStatsRepository;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TeamStats;
import biz.thonbecker.personal.foosball.infrastructure.persistence.TeamStatsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class FoosballService {

    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final TeamStatsRepository teamStatsRepository;
    private final RatingService ratingService;

    @Autowired
    public FoosballService(
            PlayerRepository playerRepository,
            GameRepository gameRepository,
            PlayerStatsRepository playerStatsRepository,
            TeamStatsRepository teamStatsRepository,
            RatingService ratingService) {
        this.playerRepository = playerRepository;
        this.gameRepository = gameRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.teamStatsRepository = teamStatsRepository;
        this.ratingService = ratingService;
    }

    // Player management
    public Player createPlayer(String name) {
        final var player = new Player(name);
        return playerRepository.save(player);
    }

    public Optional<Player> findPlayerByName(String name) {
        return playerRepository.findByName(name);
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAllByOrderByNameAsc();
    }

    // Game management
    public Game recordGame(
            Player whiteTeamPlayer1,
            Player whiteTeamPlayer2,
            Player blackTeamPlayer1,
            Player blackTeamPlayer2,
            Game.TeamColor winner) {
        final var game = new Game(whiteTeamPlayer1, whiteTeamPlayer2, blackTeamPlayer1, blackTeamPlayer2);
        game.setWinner(winner);
        final var savedGame = gameRepository.save(game);

        // Update player ratings based on a game result
        ratingService.updateRatingsAfterGame(savedGame);
        log.info("Updated ratings for game {}", savedGame.getId());

        return savedGame;
    }

    public List<Game> getAllGames() {
        final var games = new ArrayList<Game>();
        gameRepository.findAll().forEach(games::add);
        return games;
    }

    public Optional<Game> getGameById(Long id) {
        return gameRepository.findById(id);
    }

    public List<GameWithPlayers> getRecentGames() {
        return gameRepository.findRecentGames();
    }

    public List<GameWithPlayers> getLastGame() {
        final var result = gameRepository.findLastGame();
        if (!result.isEmpty()) {
            final var game = result.getFirst();
            log.info(
                    "Repository returned GameWithPlayers: id={}, wp1={}, wp2={}, bp1={}, bp2={}",
                    game.getId(),
                    game.getWhiteTeamPlayer1Name(),
                    game.getWhiteTeamPlayer2Name(),
                    game.getBlackTeamPlayer1Name(),
                    game.getBlackTeamPlayer2Name());
        }
        return result;
    }

    // Player statistics
    public List<PlayerStats> getTopPlayersByWinPercentage(int minGames) {
        return playerStatsRepository.findTopPlayersByWinPercentage(minGames);
    }

    public List<PlayerStats> getTopPlayersByTotalGames(int minGames) {
        return playerStatsRepository.findTopPlayersByTotalGames(minGames);
    }

    public List<PlayerStats> getTopPlayersByWins(int minGames) {
        return playerStatsRepository.findTopPlayersByWins(minGames);
    }

    public List<PlayerStats> getAllPlayerStatsOrderedByWinPercentage() {
        return playerStatsRepository.findAllPlayerStatsOrderedByWinPercentage();
    }

    /**
     * Get leaderboard (top 10 players by rating)
     */
    public List<Player> getLeaderboard() {
        return playerRepository.findTop10ByOrderByRatingDesc();
    }

    // Team performance statistics
    public List<TeamStats> getTopTeamsByWinPercentage(int minGames) {
        return teamStatsRepository.findTopTeamsByWinPercentage(minGames);
    }

    public List<TeamStats> getTopTeamsByAverageScore(int minGames) {
        return teamStatsRepository.findTopTeamsByAverageScore(minGames);
    }

    public List<TeamStats> getAllTeamStatsOrderedByWinPercentage() {
        return teamStatsRepository.findAllTeamStatsOrderedByWinPercentage();
    }

    // Overall statistics
    public Long getTotalGames() {
        return gameRepository.count();
    }

    public Long getTotalPlayers() {
        return playerRepository.count();
    }

    public Long getGamesWithWinner() {
        return gameRepository.countGamesWithWinner();
    }

    public Double getAverageTotalScore() {
        return gameRepository.getAverageTotalScore();
    }

    public Integer getHighestTotalScore() {
        return gameRepository.getHighestTotalScore();
    }

    public Integer getLowestTotalScore() {
        return gameRepository.getLowestTotalScore();
    }
}
