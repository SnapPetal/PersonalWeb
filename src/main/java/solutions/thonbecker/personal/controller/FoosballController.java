package solutions.thonbecker.personal.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import solutions.thonbecker.personal.service.FoosballService;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;
import solutions.thonbecker.personal.types.FoosballTeamStats;

@Controller
@RequestMapping("/foosball")
public class FoosballController {

    private final FoosballService foosballService;

    public FoosballController(FoosballService foosballService) {
        this.foosballService = foosballService;
    }

    @GetMapping
    public String foosballPage(Model model) {
        boolean serviceAvailable = foosballService.isServiceAvailable();
        model.addAttribute("serviceAvailable", serviceAvailable);

        if (serviceAvailable) {
            model.addAttribute("playerStats", foosballService.getPlayerStats());
            model.addAttribute("players", foosballService.getAllPlayers());
        }

        return "foosball";
    }

    @GetMapping("/players")
    public String getPlayers(Model model) {
        model.addAttribute("players", foosballService.getAllPlayers());
        return "foosball-players";
    }

    @GetMapping("/team-stats")
    public String getTeamStats(Model model) {
        model.addAttribute("teamStats", foosballService.getTeamStats());
        return "foosball-team-stats";
    }

    @PostMapping("/players")
    @ResponseBody
    public FoosballPlayer createPlayer(@RequestBody FoosballPlayer player) {
        return foosballService.createPlayer(player);
    }

    @PostMapping("/games")
    @ResponseBody
    public FoosballGame createGame(@RequestBody FoosballGame game) {
        return foosballService.createGame(game);
    }

    @GetMapping("/api/stats/teams")
    @ResponseBody
    public List<FoosballTeamStats> getTeamStats() {
        return foosballService.getTeamStats();
    }

    @GetMapping("/api/stats/players")
    @ResponseBody
    public List<FoosballStats> getPlayerStats() {
        return foosballService.getPlayerStats();
    }

    @GetMapping("/api/stats/players/all")
    @ResponseBody
    public List<FoosballStats> getAllPlayerStats() {
        return foosballService.getPlayerStats();
    }

    @GetMapping("/recent-games")
    public String getRecentGames(Model model) {
        model.addAttribute("games", foosballService.getRecentGames());
        return "foosball-recent-games";
    }
}
