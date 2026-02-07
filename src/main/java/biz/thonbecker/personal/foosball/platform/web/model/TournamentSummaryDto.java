package biz.thonbecker.personal.foosball.platform.web.model;

import biz.thonbecker.personal.foosball.platform.persistence.Tournament;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record TournamentSummaryDto(
        Long id,
        String name,
        @Nullable String description,
        Tournament.TournamentType tournamentType,
        Tournament.TournamentStatus status,
        @Nullable Integer maxParticipants,
        @Nullable Instant registrationStart,
        @Nullable Instant registrationEnd,
        @Nullable Instant startDate,
        @Nullable Instant endDate,
        @Nullable String createdByName,
        Instant createdAt,
        Long registrationsCount,
        Long activeRegistrationsCount) {

    public TournamentSummaryDto(
            Long id,
            String name,
            String description,
            Tournament.TournamentType tournamentType,
            Tournament.TournamentStatus status,
            Integer maxParticipants,
            Instant registrationStart,
            Instant registrationEnd,
            Instant startDate,
            Instant endDate,
            String createdByName,
            Instant createdAt,
            Long registrationsCount,
            Long activeRegistrationsCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tournamentType = tournamentType;
        this.status = status;
        this.maxParticipants = maxParticipants;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.registrationsCount = registrationsCount != null ? registrationsCount : 0L;
        this.activeRegistrationsCount = activeRegistrationsCount != null ? activeRegistrationsCount : 0L;
    }
}
