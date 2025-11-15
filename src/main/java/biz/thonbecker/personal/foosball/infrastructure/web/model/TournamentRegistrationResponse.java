package biz.thonbecker.personal.foosball.infrastructure.web.model;

import biz.thonbecker.personal.foosball.infrastructure.persistence.TournamentRegistration;

import java.time.LocalDateTime;

public record TournamentRegistrationResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        String playerName,
        String partnerName,
        String teamName,
        String displayName,
        LocalDateTime registrationDate,
        Integer seed,
        TournamentRegistration.RegistrationStatus status,
        boolean isTeam,
        boolean isActive) {
    public static TournamentRegistrationResponse fromEntity(TournamentRegistration registration) {
        return new TournamentRegistrationResponse(
                registration.getId(),
                registration.getTournament().getId(),
                registration.getTournament().getName(),
                registration.getPlayer().getName(),
                registration.getPartner() != null ? registration.getPartner().getName() : null,
                registration.getTeamName(),
                registration.getDisplayName(),
                registration.getRegistrationDate(),
                registration.getSeed(),
                registration.getStatus(),
                registration.isTeam(),
                registration.isActive());
    }
}
