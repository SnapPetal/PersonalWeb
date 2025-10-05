package solutions.thonbecker.personal.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import solutions.thonbecker.personal.service.FoosballService;
import solutions.thonbecker.personal.types.FoosballGame;
import solutions.thonbecker.personal.types.FoosballPlayer;

import java.util.List;

@Controller
@RequestMapping("/foosball")
@Slf4j
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

    @GetMapping("/fragments/player-table")
    public String getPlayerTableFragment(Model model) {
        model.addAttribute("players", foosballService.getAllPlayers());
        return "foosball-players :: playerTable";
    }

    @PostMapping("/htmx/players")
    public String createPlayerHtmx(
            @RequestParam String name, @RequestParam String email, Model model) {
        try {
            if (name != null
                    && !name.trim().isEmpty()
                    && email != null
                    && !email.trim().isEmpty()) {
                FoosballPlayer player = new FoosballPlayer();
                player.setName(name.trim());
                player.setEmail(email.trim());
                foosballService.createPlayer(player);
                model.addAttribute("success", "Player '" + name.trim() + "' added successfully!");
            } else {
                model.addAttribute("error", "Please provide both name and email.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Failed to add player. Please try again.");
        }

        return "foosball-fragments :: alert";
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
            // Validation
            if (whiteTeamPlayer1 == null
                    || whiteTeamPlayer1.isEmpty()
                    || whiteTeamPlayer2 == null
                    || whiteTeamPlayer2.isEmpty()
                    || blackTeamPlayer1 == null
                    || blackTeamPlayer1.isEmpty()
                    || blackTeamPlayer2 == null
                    || blackTeamPlayer2.isEmpty()) {
                model.addAttribute("error", "Please select all players.");
                return "foosball-fragments :: alert";
            }

            if (whiteTeamScore < 0 || blackTeamScore < 0) {
                model.addAttribute("error", "Scores must be non-negative.");
                return "foosball-fragments :: alert";
            }

            // Get username from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : null;

            FoosballGame game = new FoosballGame();
            game.setWhiteTeamPlayer1(whiteTeamPlayer1);
            game.setWhiteTeamPlayer2(whiteTeamPlayer2);
            game.setBlackTeamPlayer1(blackTeamPlayer1);
            game.setBlackTeamPlayer2(blackTeamPlayer2);
            game.setWhiteTeamScore(whiteTeamScore);
            game.setBlackTeamScore(blackTeamScore);
            game.setNotes(notes);
            game.setUsername(username);

            FoosballGame createdGame = foosballService.createGame(game);
            if (createdGame != null) {
                model.addAttribute("success", "Game recorded successfully!");
            } else {
                model.addAttribute(
                        "error", "Failed to record game. Server returned an empty response.");
            }
        } catch (Exception e) {
            String errorMessage = "Failed to record game: " + e.getMessage();
            // Log the detailed error
            log.error("Error recording game", e);

            // Extract meaningful message for the user
            if (e.getMessage() != null && e.getMessage().contains("400")) {
                errorMessage = "Invalid game data. Please check all fields and try again.";
            }

            model.addAttribute("error", errorMessage);
        }

        return "foosball-fragments :: alert";
    }
}
