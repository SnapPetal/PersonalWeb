package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * Projection for tournament summary information
 */
public interface TournamentSummary {
    Long getId();

    String getName();

    String getDescription();

    Tournament.TournamentType getTournamentType();

    Tournament.TournamentStatus getStatus();

    Integer getMaxParticipants();

    LocalDateTime getRegistrationStart();

    LocalDateTime getRegistrationEnd();

    LocalDateTime getStartDate();

    LocalDateTime getEndDate();

    String getCreatedByName();

    LocalDateTime getCreatedAt();

    Integer getRegistrationsCount();

    Integer getActiveRegistrationsCount();
}
