package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.time.Instant;

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

    Instant getRegistrationStart();

    Instant getRegistrationEnd();

    Instant getStartDate();

    Instant getEndDate();

    String getCreatedByName();

    Instant getCreatedAt();

    Integer getRegistrationsCount();

    Integer getActiveRegistrationsCount();
}
