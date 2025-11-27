package biz.thonbecker.personal.foosball.infrastructure.web;

import biz.thonbecker.personal.foosball.api.FoosballFacade;
import biz.thonbecker.personal.foosball.domain.Game;
import biz.thonbecker.personal.foosball.domain.GameResult;
import biz.thonbecker.personal.foosball.domain.Player;
import biz.thonbecker.personal.foosball.domain.Team;
import biz.thonbecker.personal.foosball.infrastructure.TournamentService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Foosball controller - handles web endpoints for foosball functionality.
 * Delegates to FoosballFacade for business logic.
 */
@Controller
@RequestMapping("/foosball")
@Slf4j
public class FoosballController {

    private final FoosballFacade foosballFacade;
    private final TournamentService tournamentService;

    public FoosballController(FoosballFacade foosballFacade, TournamentService tournamentService) {
        this.foosballFacade = foosballFacade;
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public String foosballPage(Model model) {
        boolean serviceAvailable = foosballFacade.isServiceAvailable();
        model.addAttribute("serviceAvailable", serviceAvailable);

        if (serviceAvailable) {
            model.addAttribute("playerStats", foosballFacade.getPlayerStats());
            model.addAttribute("players", foosballFacade.getAllPlayers());
        }

        return "foosball";
    }

    @GetMapping("/players")
    public String getPlayers(Model model) {
        model.addAttribute("players", foosballFacade.getAllPlayers());
        return "foosball-players";
    }

    @GetMapping("/team-stats")
    public String getTeamStats(Model model) {
        model.addAttribute("teamStats", foosballFacade.getTeamStats());
        return "foosball-team-stats";
    }

    @GetMapping("/recent-games")
    public String getRecentGames(Model model) {
        model.addAttribute("games", foosballFacade.getRecentGames());
        model.addAttribute("ResultStatus", GameResult.class);
        return "foosball-recent-games";
    }

    @GetMapping("/tournaments")
    public String tournaments(Model model) {
        model.addAttribute("players", foosballFacade.getAllPlayers());
        return "foosball-tournaments";
    }

    @GetMapping("/tournaments/{id}")
    public String tournamentDetail(@PathVariable Long id, Model model) {
        var tournament = tournamentService.getTournamentWithRegistrations(id);
        var bracket = tournamentService.getBracketView(id);
        var registrations = tournamentService.getTournamentRegistrations(id);
        var matches = tournamentService.getTournamentMatches(id);

        model.addAttribute("tournament", tournament);
        model.addAttribute("bracket", bracket);
        model.addAttribute("registrations", registrations);
        model.addAttribute("matches", matches);
        model.addAttribute("players", foosballFacade.getAllPlayers());

        return "foosball-tournament-detail";
    }

    // HTMX Fragment Endpoints
    @GetMapping("/fragments/player-stats")
    public String getPlayerStatsFragment(Model model) {
        model.addAttribute("playerStats", foosballFacade.getPlayerStats());
        return "foosball-fragments :: playerStatsList";
    }

    @GetMapping("/fragments/games-list")
    public String getGamesListFragment(Model model) {
        model.addAttribute("games", foosballFacade.getRecentGames());
        return "foosball-fragments :: gamesList";
    }

    @GetMapping("/fragments/player-options")
    public String getPlayerOptionsFragment(Model model) {
        try {
            model.addAttribute("players", foosballFacade.getAllPlayers());
        } catch (Exception e) {
            // If service is unavailable, provide empty list
            model.addAttribute("players", List.of());
        }
        return "foosball-fragments :: playerOptions";
    }

    @GetMapping("/fragments/last-game-teams")
    public String getLastGameTeamsFragment(Model model) {
        try {
            var lastGame = foosballFacade.getLastGame();
            if (lastGame != null) {
                String wp1 = lastGame.getWhiteTeam().getPlayer1();
                String wp2 = lastGame.getWhiteTeam().getPlayer2();
                String bp1 = lastGame.getBlackTeam().getPlayer1();
                String bp2 = lastGame.getBlackTeam().getPlayer2();

                log.info("Last game teams - White: {} & {}, Black: {} & {}", wp1, wp2, bp1, bp2);

                model.addAttribute("whiteTeamPlayer1", wp1);
                model.addAttribute("whiteTeamPlayer2", wp2);
                model.addAttribute("blackTeamPlayer1", bp1);
                model.addAttribute("blackTeamPlayer2", bp2);
            }
        } catch (Exception e) {
            log.error("Error loading last game teams", e);
        }
        return "foosball-fragments :: lastGameTeams";
    }

    @GetMapping("/fragments/player-table")
    public String getPlayerTableFragment(Model model) {
        model.addAttribute("players", foosballFacade.getAllPlayers());
        return "foosball-players :: playerTable";
    }

    @PostMapping("/htmx/players")
    public String createPlayerHtmx(@RequestParam String name, @RequestParam String email, Model model) {
        try {
            if (name != null
                    && !name.trim().isEmpty()
                    && email != null
                    && !email.trim().isEmpty()) {
                Player player = new Player(name.trim());
                foosballFacade.createPlayer(player);
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
            @RequestParam String winner,
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

            if (winner == null || winner.isEmpty()) {
                model.addAttribute("error", "Please select a winner.");
                return "foosball-fragments :: alert";
            }

            // Create game using new domain model
            Team whiteTeam = new Team(whiteTeamPlayer1, whiteTeamPlayer2);
            Team blackTeam = new Team(blackTeamPlayer1, blackTeamPlayer2);

            // Map winner string to GameResult
            biz.thonbecker.personal.foosball.domain.GameResult result;
            switch (winner) {
                case "TEAM1":
                    result = biz.thonbecker.personal.foosball.domain.GameResult.WHITE_TEAM_WIN;
                    break;
                case "TEAM2":
                    result = biz.thonbecker.personal.foosball.domain.GameResult.BLACK_TEAM_WIN;
                    break;
                default:
                    model.addAttribute("error", "Invalid winner value.");
                    return "foosball-fragments :: alert";
            }

            Game game = new Game(whiteTeam, blackTeam, result);

            Game createdGame = foosballFacade.createGame(game);
            if (createdGame != null) {
                model.addAttribute("success", "Game recorded successfully!");
            } else {
                model.addAttribute("error", "Failed to record game. Server returned an empty response.");
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
