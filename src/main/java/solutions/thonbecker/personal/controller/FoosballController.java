package solutions.thonbecker.personal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import solutions.thonbecker.personal.service.FoosballService;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;
import solutions.thonbecker.personal.types.FoosballStats;
import solutions.thonbecker.personal.types.FoosballTeamStats;

import java.util.List;

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

    // HTMX Fragment Endpoints
    @GetMapping("/fragments/player-stats")
    public String getPlayerStatsFragment(Model model) {
        model.addAttribute("playerStats", foosballService.getPlayerStats());
        return "foosball-fragments :: playerStatsList";
    }

    @GetMapping("/fragments/games-list")
    public String getGamesListFragment(Model model) {
        model.addAttribute("games", foosballService.getRecentGames());
        return "foosball-fragments :: gamesList";
    }

    @GetMapping("/fragments/player-options")
    public String getPlayerOptionsFragment(Model model) {
        try {
            model.addAttribute("players", foosballService.getAllPlayers());
        } catch (Exception e) {
            // If service is unavailable, provide empty list
            model.addAttribute("players", List.of());
        }
        return "foosball-fragments :: playerOptions";
    }

    @PostMapping("/htmx/players")
    public String createPlayerHtmx(
            @RequestParam String name, @RequestParam String email, Model model) {
        if (name != null
                && !name.trim().isEmpty()
                && email != null
                && !email.trim().isEmpty()) {
            FoosballPlayer player = new FoosballPlayer();
            player.setName(name.trim());
            player.setEmail(email.trim());
            foosballService.createPlayer(player);
        }

        // Return updated player options for the dropdowns
        model.addAttribute("players", foosballService.getAllPlayers());
        model.addAttribute("success", "Player added successfully!");
        return "foosball-fragments :: playerOptions";
    }

    @PostMapping("/htmx/games")
    public String createGameHtmx(
            @RequestParam String whiteTeamPlayer1,
            @RequestParam String whiteTeamPlayer2,
            @RequestParam String blackTeamPlayer1,
            @RequestParam String blackTeamPlayer2,
            @RequestParam int whiteTeamScore,
            @RequestParam int blackTeamScore,
            @RequestParam(required = false) String notes,
            Model model) {

        try {
            FoosballGame game = new FoosballGame();
            game.setWhiteTeamPlayer1(whiteTeamPlayer1);
            game.setWhiteTeamPlayer2(whiteTeamPlayer2);
            game.setBlackTeamPlayer1(blackTeamPlayer1);
            game.setBlackTeamPlayer2(blackTeamPlayer2);
            game.setWhiteTeamScore(whiteTeamScore);
            game.setBlackTeamScore(blackTeamScore);
            game.setNotes(notes);

            foosballService.createGame(game);
            model.addAttribute("success", "Game recorded successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to record game. Please try again.");
        }

        // Return updated games list
        model.addAttribute("games", foosballService.getRecentGames());
        return "foosball-fragments :: gamesList";
    }
}
