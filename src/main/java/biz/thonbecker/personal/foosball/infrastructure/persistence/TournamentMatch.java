package biz.thonbecker.personal.foosball.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(
        exclude = {"tournament", "team1", "team2", "winner", "game", "nextMatch", "consolationMatch"
        })
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(
        name = "tournament_matches",
        schema = "foosball",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tournament_round_match",
                    columnNames = {"tournament_id", "round_number", "match_number"})
        })
@EntityListeners(AuditingEntityListener.class)
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tournament is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonBackReference
    private Tournament tournament;

    @NotNull(message = "Round number is required")
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @NotNull(message = "Match number is required")
    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "bracket_type", nullable = false)
    private BracketType bracketType = BracketType.MAIN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_registration_id")
    private TournamentRegistration team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_registration_id")
    private TournamentRegistration team2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_registration_id")
    private TournamentRegistration winner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Navigation for bracket progression
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private TournamentMatch nextMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolation_match_id")
    private TournamentMatch consolationMatch;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Enums
    public enum MatchStatus {
        PENDING, // Match created but not ready to play
        READY, // Both teams assigned, ready to play
        IN_PROGRESS, // Game is being played
        COMPLETED, // Match finished
        WALKOVER, // One team didn't show up
        CANCELLED // Match cancelled
    }

    public enum BracketType {
        MAIN, // Main bracket (winners bracket in double elimination)
        LOSERS, // Losers bracket in double elimination
        CONSOLATION, // Consolation matches
        PLAYOFF // Playoff matches for positioning
    }

    // Constructors
    public TournamentMatch(Tournament tournament, Integer roundNumber, Integer matchNumber) {
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.matchNumber = matchNumber;
        this.status = MatchStatus.PENDING;
        this.bracketType = BracketType.MAIN;
    }

    public TournamentMatch(
            Tournament tournament,
            Integer roundNumber,
            Integer matchNumber,
            TournamentRegistration team1,
            TournamentRegistration team2) {
        this(tournament, roundNumber, matchNumber);
        this.team1 = team1;
        this.team2 = team2;
        updateStatus();
    }

    // Business logic methods
    public boolean isReady() {
        return team1 != null && team2 != null && team1.isActive() && team2.isActive();
    }

    public boolean isCompleted() {
        return status == MatchStatus.COMPLETED || status == MatchStatus.WALKOVER;
    }

    public boolean canStart() {
        return isReady() && status == MatchStatus.READY;
    }

    public void updateStatus() {
        if (status == MatchStatus.COMPLETED || status == MatchStatus.WALKOVER) {
            return; // Don't change completed matches
        }

        if (isReady()) {
            this.status = MatchStatus.READY;
        } else {
            this.status = MatchStatus.PENDING;
        }
    }

    public void start() {
        if (canStart()) {
            this.status = MatchStatus.IN_PROGRESS;
        }
    }

    public void complete(Game gameResult) {
        if (gameResult != null && gameResult.getWinner() != null) {
            this.game = gameResult;
            this.status = MatchStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();

            // Determine winner based on game result
            determineWinner(gameResult);
        }
    }

    public void walkover(TournamentRegistration walkoverWinner) {
        if (walkoverWinner != null
                && (walkoverWinner.equals(team1) || walkoverWinner.equals(team2))) {
            this.winner = walkoverWinner;
            this.status = MatchStatus.WALKOVER;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void cancel() {
        this.status = MatchStatus.CANCELLED;
    }

    private void determineWinner(Game gameResult) {
        if (gameResult.getWinner() == null) {
            // Handle draws based on tournament settings
            return;
        }

        // Match players from game to tournament registrations
        boolean team1IsWhite = matchesTeam(
                team1, gameResult.getWhiteTeamPlayer1(), gameResult.getWhiteTeamPlayer2());
        boolean team1IsBlack = matchesTeam(
                team1, gameResult.getBlackTeamPlayer1(), gameResult.getBlackTeamPlayer2());

        if (team1IsWhite && gameResult.isWhiteTeamWinner()) {
            this.winner = team1;
        } else if (team1IsBlack && gameResult.isBlackTeamWinner()) {
            this.winner = team1;
        } else {
            this.winner = team2;
        }
    }

    private boolean matchesTeam(
            TournamentRegistration registration, Player player1, Player player2) {
        if (registration.isTeam()) {
            return (registration.getPlayer().equals(player1)
                            && registration.getPartner().equals(player2))
                    || (registration.getPlayer().equals(player2)
                            && registration.getPartner().equals(player1));
        } else {
            return registration.getPlayer().equals(player1)
                    || registration.getPlayer().equals(player2);
        }
    }

    public TournamentRegistration getLoser() {
        if (winner == null || !isCompleted()) {
            return null;
        }
        return winner.equals(team1) ? team2 : team1;
    }

    public String getDisplayName() {
        String bracket = bracketType == BracketType.MAIN
                ? ""
                : " (" + bracketType.name().toLowerCase() + ")";
        return "Round " + roundNumber + ", Match " + matchNumber + bracket;
    }

    public String getMatchDescription() {
        String team1Name = team1 != null ? team1.getDisplayName() : "TBD";
        String team2Name = team2 != null ? team2.getDisplayName() : "TBD";
        return team1Name + " vs " + team2Name;
    }

    // Helper methods for bracket navigation
    public boolean hasBye() {
        return (team1 == null) ^ (team2 == null); // XOR - exactly one is null
    }

    public TournamentRegistration getByeWinner() {
        if (hasBye()) {
            return team1 != null ? team1 : team2;
        }
        return null;
    }

    public void assignTeam1(TournamentRegistration registration) {
        this.team1 = registration;
        updateStatus();
    }

    public void assignTeam2(TournamentRegistration registration) {
        this.team2 = registration;
        updateStatus();
    }
}
