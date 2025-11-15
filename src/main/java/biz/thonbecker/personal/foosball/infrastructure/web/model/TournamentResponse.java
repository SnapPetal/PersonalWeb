package biz.thonbecker.personal.foosball.infrastructure.web.model;

import biz.thonbecker.personal.foosball.infrastructure.persistence.Tournament;

import java.time.LocalDateTime;
import java.util.List;

public record TournamentResponse(
        Long id,
        String name,
        String description,
        Tournament.TournamentType tournamentType,
        Tournament.TournamentStatus status,
        Integer maxParticipants,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String createdBy,
        Tournament.TournamentSettings settings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int registrationsCount,
        int activeRegistrationsCount,
        boolean canRegister,
        boolean canStart,
        List<TournamentRegistrationResponse> registrations) {
    public static TournamentResponse fromEntity(Tournament tournament) {
        List<TournamentRegistrationResponse> registrationResponses =
                tournament.getRegistrations().stream()
                        .map(TournamentRegistrationResponse::fromEntity)
                        .toList();

        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getTournamentType(),
                tournament.getStatus(),
                tournament.getMaxParticipants(),
                tournament.getRegistrationStart(),
                tournament.getRegistrationEnd(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getCreatedBy().getName(),
                tournament.getSettings(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt(),
                tournament.getRegistrations().size(),
                tournament.getActiveRegistrationsCount(),
                tournament.canRegister(),
                tournament.canStart(),
                registrationResponses);
    }

    public static TournamentResponse fromEntitySummary(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getTournamentType(),
                tournament.getStatus(),
                tournament.getMaxParticipants(),
                tournament.getRegistrationStart(),
                tournament.getRegistrationEnd(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getCreatedBy().getName(),
                tournament.getSettings(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt(),
                tournament.getRegistrations().size(),
                tournament.getActiveRegistrationsCount(),
                tournament.canRegister(),
                tournament.canStart(),
                null // Don't include registrations in summary
                );
    }
}
