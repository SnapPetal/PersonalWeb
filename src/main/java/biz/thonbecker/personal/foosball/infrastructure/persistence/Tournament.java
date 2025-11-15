package biz.thonbecker.personal.foosball.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"registrations", "matches", "standings"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(name = "tournaments", schema = "foosball")
@EntityListeners(AuditingEntityListener.class)
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tournament name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Tournament type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "tournament_type", nullable = false)
    private TournamentType tournamentType;

    @NotNull(message = "Tournament status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TournamentStatus status = TournamentStatus.DRAFT;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "registration_start")
    private LocalDateTime registrationStart;

    @Column(name = "registration_end")
    private LocalDateTime registrationEnd;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @NotNull(message = "Tournament creator is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Player createdBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private TournamentSettings settings;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TournamentRegistration> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TournamentMatch> matches = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TournamentStanding> standings = new ArrayList<>();

    // Enums
    public enum TournamentType {
        SINGLE_ELIMINATION,
        DOUBLE_ELIMINATION,
        ROUND_ROBIN,
        SWISS_SYSTEM,
        LADDER
    }

    public enum TournamentStatus {
        DRAFT,
        REGISTRATION_OPEN,
        REGISTRATION_CLOSED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    // Constructors
    public Tournament(String name, TournamentType tournamentType, Player createdBy) {
        this.name = name;
        this.tournamentType = tournamentType;
        this.createdBy = createdBy;
        this.status = TournamentStatus.DRAFT;
        this.settings = new TournamentSettings();
    }

    // Business logic methods
    public boolean isRegistrationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return status == TournamentStatus.REGISTRATION_OPEN
                && (registrationStart == null || !now.isBefore(registrationStart))
                && (registrationEnd == null || !now.isAfter(registrationEnd));
    }

    public boolean canRegister() {
        return isRegistrationOpen()
                && (maxParticipants == null || registrations.size() < maxParticipants);
    }

    public boolean canStart() {
        return status == TournamentStatus.REGISTRATION_CLOSED
                && !registrations.isEmpty()
                && registrations.size() >= getMinimumParticipants();
    }

    public int getMinimumParticipants() {
        return switch (tournamentType) {
            case SINGLE_ELIMINATION, DOUBLE_ELIMINATION -> 2;
            case ROUND_ROBIN, SWISS_SYSTEM -> 3;
            case LADDER -> 2;
        };
    }

    public void openRegistration() {
        if (status == TournamentStatus.DRAFT) {
            this.status = TournamentStatus.REGISTRATION_OPEN;
        }
    }

    public void closeRegistration() {
        if (status == TournamentStatus.REGISTRATION_OPEN) {
            this.status = TournamentStatus.REGISTRATION_CLOSED;
        }
    }

    public void start() {
        if (canStart()) {
            this.status = TournamentStatus.IN_PROGRESS;
        }
    }

    public void complete() {
        if (status == TournamentStatus.IN_PROGRESS) {
            this.status = TournamentStatus.COMPLETED;
            this.endDate = LocalDateTime.now();
        }
    }

    public void cancel() {
        if (status != TournamentStatus.COMPLETED) {
            this.status = TournamentStatus.CANCELLED;
        }
    }

    // Helper methods
    public int getActiveRegistrationsCount() {
        return (int) registrations.stream()
                .filter(reg -> reg.getStatus() == TournamentRegistration.RegistrationStatus.ACTIVE)
                .count();
    }

    public boolean isElimination() {
        return tournamentType == TournamentType.SINGLE_ELIMINATION
                || tournamentType == TournamentType.DOUBLE_ELIMINATION;
    }

    public boolean isRoundBased() {
        return tournamentType == TournamentType.ROUND_ROBIN
                || tournamentType == TournamentType.SWISS_SYSTEM;
    }

    // Tournament Settings class for JSON storage
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TournamentSettings {
        private Integer pointsForWin = 3;
        private Integer pointsForDraw = 1;
        private Integer pointsForLoss = 0;
        private Integer swissRounds = 5;
        private Boolean allowDraws = true;
        private Integer matchDuration = 30; // minutes
        private Boolean autoAdvancement = true;
        private Integer breakBetweenRounds = 10; // minutes
    }
}
