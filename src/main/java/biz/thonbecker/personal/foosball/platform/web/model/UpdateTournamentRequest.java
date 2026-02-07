package biz.thonbecker.personal.foosball.platform.web.model;

import biz.thonbecker.personal.foosball.platform.persistence.Tournament;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record UpdateTournamentRequest(
        String name,
        String description,
        Tournament.TournamentType tournamentType,

        @Positive(message = "Maximum participants must be positive")
        Integer maxParticipants,

        @Future(message = "Registration start must be in the future")
        Instant registrationStart,

        @Future(message = "Registration end must be in the future")
        Instant registrationEnd,

        @Future(message = "Start date must be in the future")
        Instant startDate,

        Tournament.TournamentSettings settings) {
    public UpdateTournamentRequest {
        // Validation: registrationEnd should be after registrationStart
        if (registrationStart != null && registrationEnd != null && !registrationEnd.isAfter(registrationStart)) {
            throw new IllegalArgumentException("Registration end must be after registration start");
        }

        // Validation: startDate should be after registrationEnd
        if (registrationEnd != null && startDate != null && !startDate.isAfter(registrationEnd)) {
            throw new IllegalArgumentException("Tournament start must be after registration end");
        }
    }
}
