package biz.thonbecker.personal.foosball.infrastructure.web.model;

import jakarta.validation.constraints.NotNull;

public record WalkoverRequest(
        @NotNull(message = "Winner registration ID is required")
        Long winnerRegistrationId,

        String reason) {}
