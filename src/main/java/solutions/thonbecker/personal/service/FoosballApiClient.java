package solutions.thonbecker.personal.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;
import solutions.thonbecker.personal.types.FoosballTeamStats;

import java.util.List;

@FeignClient(
        name = "foosball-api",
        url = "${foosball.api.base-url}",
        configuration = FoosballFeignConfig.class)
public interface FoosballApiClient {

    @GetMapping("/api/foosball/players")
    List<FoosballPlayer> getAllPlayers();

    @PostMapping("/api/foosball/players")
    FoosballPlayer createPlayer(@RequestBody FoosballPlayer player);

    @GetMapping("/api/foosball/stats/teams/all")
    List<FoosballTeamStats> getTeamStats();

    @PostMapping("/api/foosball/games")
    FoosballGame createGame(@RequestBody FoosballGame game);

    @GetMapping("/api/foosball/stats/players/all")
    List<FoosballStats> getPlayerStats();

    @GetMapping("/api/foosball/games/recent")
    List<FoosballGame> getRecentGames();

    @GetMapping("/actuator/health")
    Object checkHealth();
}
