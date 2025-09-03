package solutions.thonbecker.personal.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import solutions.thonbecker.personal.service.FoosballService;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;

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
            List<FoosballPlayer> players = foosballService.getAllPlayers();
            List<FoosballGame> games = foosballService.getAllGames();
            List<FoosballStats> playerStats = foosballService.getPlayerStats();
            List<FoosballStats> positionStats = foosballService.getPositionStats();

            model.addAttribute("players", players);
            model.addAttribute("games", games);
            model.addAttribute("playerStats", playerStats);
            model.addAttribute("positionStats", positionStats);
        }

        return "foosball";
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

    @GetMapping("/api/players")
    @ResponseBody
    public List<FoosballPlayer> getPlayers() {
        return foosballService.getAllPlayers();
    }

    @GetMapping("/api/games")
    @ResponseBody
    public List<FoosballGame> getGames() {
        return foosballService.getAllGames();
    }

    @GetMapping("/api/stats/players")
    @ResponseBody
    public List<FoosballStats> getPlayerStats() {
        return foosballService.getPlayerStats();
    }

    @GetMapping("/api/stats/position")
    @ResponseBody
    public List<FoosballStats> getPositionStats() {
        return foosballService.getPositionStats();
    }


}
