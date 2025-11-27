package biz.thonbecker.personal.foosball.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@ToString(exclude = {"tournament", "player", "partner"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(
        name = "tournament_registrations",
        schema = "foosball",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tournament_player",
                    columnNames = {"tournament_id", "player_id"})
        })
@EntityListeners(AuditingEntityListener.class)
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tournament is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonBackReference
    private Tournament tournament;

    @NotNull(message = "Player is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Player partner;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @CreatedDate
    @Column(name = "registration_date", nullable = false, updatable = false)
    private Instant registrationDate;

    @Column(name = "seed")
    private Integer seed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    // Enums
    public enum RegistrationStatus {
        ACTIVE,
        WITHDRAWN,
        DISQUALIFIED
    }

    // Constructors
    public TournamentRegistration(Tournament tournament, Player player) {
        this.tournament = tournament;
        this.player = player;
        this.status = RegistrationStatus.ACTIVE;
    }

    public TournamentRegistration(Tournament tournament, Player player, Player partner) {
        this(tournament, player);
        this.partner = partner;
        generateTeamName();
    }

    public TournamentRegistration(Tournament tournament, Player player, Player partner, String teamName) {
        this(tournament, player, partner);
        this.teamName = teamName;
    }

    // Business logic methods
    public boolean isTeam() {
        return partner != null;
    }

    public boolean isActive() {
        return status == RegistrationStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = RegistrationStatus.WITHDRAWN;
    }

    public void disqualify() {
        this.status = RegistrationStatus.DISQUALIFIED;
    }

    public void reactivate() {
        if (tournament.canRegister()) {
            this.status = RegistrationStatus.ACTIVE;
        }
    }

    public String getDisplayName() {
        if (teamName != null && !teamName.trim().isEmpty()) {
            return teamName;
        }
        if (isTeam()) {
            return player.getName() + " & " + partner.getName();
        }
        return player.getName();
    }

    public String generateTeamName() {
        if (isTeam()) {
            String generatedName = player.getName() + " & " + partner.getName();
            if (teamName == null || teamName.trim().isEmpty()) {
                this.teamName = generatedName;
            }
            return generatedName;
        }
        return player.getName();
    }

    // Helper methods for match assignments
    public boolean containsPlayer(Player checkPlayer) {
        return player.equals(checkPlayer) || (partner != null && partner.equals(checkPlayer));
    }

    public boolean canPlayAgainst(TournamentRegistration opponent) {
        if (opponent == null || !this.isActive() || !opponent.isActive()) {
            return false;
        }

        // Can't play against yourself
        if (this.equals(opponent)) {
            return false;
        }

        // Can't play if you share a player
        return !this.containsPlayer(opponent.player)
                && (opponent.partner == null || !this.containsPlayer(opponent.partner));
    }
}
