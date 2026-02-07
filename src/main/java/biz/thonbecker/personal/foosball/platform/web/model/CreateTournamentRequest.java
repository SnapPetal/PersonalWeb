package biz.thonbecker.personal.foosball.platform.web.model;

import biz.thonbecker.personal.foosball.platform.persistence.Tournament;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record CreateTournamentRequest(
        @NotBlank(message = "Tournament name is required") String name,
        String description,
        @NotNull(message = "Tournament type is required") Tournament.TournamentType tournamentType,

        @Positive(message = "Maximum participants must be positive")
        Integer maxParticipants,

        @Future(message = "Registration start must be in the future")
        Instant registrationStart,

        @Future(message = "Registration end must be in the future")
        Instant registrationEnd,

        @Future(message = "Start date must be in the future")
        Instant startDate,

        Tournament.TournamentSettings settings) {
    public CreateTournamentRequest {
        // Validation: registrationEnd should be after registrationStart
        if (registrationStart != null && registrationEnd != null && !registrationEnd.isAfter(registrationStart)) {
            throw new IllegalArgumentException("Registration end must be after registration start");
        }

        // Validation: startDate should be after registrationEnd
        if (registrationEnd != null && startDate != null && !startDate.isAfter(registrationEnd)) {
            throw new IllegalArgumentException("Tournament start must be after registration end");
        }

        // Set default settings if none provided
        if (settings == null) {
            settings = new Tournament.TournamentSettings();
        }
    }
}
