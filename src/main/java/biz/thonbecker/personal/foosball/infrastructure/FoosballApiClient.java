package biz.thonbecker.personal.foosball.infrastructure;

import biz.thonbecker.personal.foosball.domain.FoosballGame;
import biz.thonbecker.personal.foosball.domain.FoosballPlayer;
import biz.thonbecker.personal.foosball.domain.FoosballStats;
import biz.thonbecker.personal.foosball.domain.FoosballTeamStats;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "foosball-api", url = "${foosball.api.base-url}")
interface FoosballApiClient {

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
