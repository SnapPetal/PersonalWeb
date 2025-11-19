package biz.thonbecker.personal.foosball.infrastructure.web.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchScoreRequest(
        @NotNull(message = "Team 1 score is required") @Min(value = 0, message = "Score cannot be negative")
                Integer team1Score,
        @NotNull(message = "Team 2 score is required") @Min(value = 0, message = "Score cannot be negative")
                Integer team2Score) {}
