package biz.thonbecker.personal.foosball.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"tournament", "registration"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(
        name = "tournament_standings",
        schema = "foosball",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tournament_registration_standing",
                    columnNames = {"tournament_id", "registration_id"})
        })
@EntityListeners(AuditingEntityListener.class)
public class TournamentStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tournament is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonBackReference
    private Tournament tournament;

    @NotNull(message = "Registration is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private TournamentRegistration registration;

    @Column(name = "position")
    private Integer position;

    @Column(name = "points", precision = 10, scale = 2, nullable = false)
    private BigDecimal points = BigDecimal.ZERO;

    @Column(name = "wins", nullable = false)
    private Integer wins = 0;

    @Column(name = "losses", nullable = false)
    private Integer losses = 0;

    @Column(name = "draws", nullable = false)
    private Integer draws = 0;

    @Column(name = "games_played", nullable = false)
    private Integer gamesPlayed = 0;

    @Column(name = "goals_for", nullable = false)
    private Integer goalsFor = 0;

    @Column(name = "goals_against", nullable = false)
    private Integer goalsAgainst = 0;

    @Column(name = "goal_difference", nullable = false)
    private Integer goalDifference = 0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public TournamentStanding(Tournament tournament, TournamentRegistration registration) {
        this.tournament = tournament;
        this.registration = registration;
        resetStats();
    }

    // Business logic methods
    public void resetStats() {
        this.points = BigDecimal.ZERO;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.gamesPlayed = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
        this.goalDifference = 0;
    }

    public void recordWin(int goalsFor, int goalsAgainst) {
        this.wins++;
        this.gamesPlayed++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        updateGoalDifference();
        addPoints(tournament.getSettings().getPointsForWin());
    }

    public void recordLoss(int goalsFor, int goalsAgainst) {
        this.losses++;
        this.gamesPlayed++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        updateGoalDifference();
        addPoints(tournament.getSettings().getPointsForLoss());
    }

    public void recordDraw(int goalsFor, int goalsAgainst) {
        this.draws++;
        this.gamesPlayed++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        updateGoalDifference();
        addPoints(tournament.getSettings().getPointsForDraw());
    }

    public void recordMatch(TournamentMatch match) {
        if (match.getGame() == null || !match.isCompleted()) {
            return;
        }

        Game game = match.getGame();
        boolean isRegistrationTeam1 = match.getTeam1().equals(registration);

        int teamGoals, opponentGoals;
        if (isRegistrationTeam1) {
            // Determine which team color this registration played as
            boolean isWhiteTeam = matchesWhiteTeam(registration, game);
            teamGoals = isWhiteTeam ? game.getWhiteTeamScore() : game.getBlackTeamScore();
            opponentGoals = isWhiteTeam ? game.getBlackTeamScore() : game.getWhiteTeamScore();
        } else {
            // This registration is team2
            boolean isWhiteTeam = matchesWhiteTeam(registration, game);
            teamGoals = isWhiteTeam ? game.getWhiteTeamScore() : game.getBlackTeamScore();
            opponentGoals = isWhiteTeam ? game.getBlackTeamScore() : game.getWhiteTeamScore();
        }

        if (match.getWinner() != null) {
            if (match.getWinner().equals(registration)) {
                recordWin(teamGoals, opponentGoals);
            } else {
                recordLoss(teamGoals, opponentGoals);
            }
        } else {
            recordDraw(teamGoals, opponentGoals);
        }
    }

    private boolean matchesWhiteTeam(TournamentRegistration reg, Game game) {
        if (reg.isTeam()) {
            return (reg.getPlayer().equals(game.getWhiteTeamPlayer1())
                            && reg.getPartner().equals(game.getWhiteTeamPlayer2()))
                    || (reg.getPlayer().equals(game.getWhiteTeamPlayer2())
                            && reg.getPartner().equals(game.getWhiteTeamPlayer1()));
        } else {
            return reg.getPlayer().equals(game.getWhiteTeamPlayer1())
                    || reg.getPlayer().equals(game.getWhiteTeamPlayer2());
        }
    }

    private void updateGoalDifference() {
        this.goalDifference = this.goalsFor - this.goalsAgainst;
    }

    private void addPoints(int pointsToAdd) {
        this.points = this.points.add(BigDecimal.valueOf(pointsToAdd));
    }

    // Calculated properties
    public double getWinPercentage() {
        return gamesPlayed == 0 ? 0.0 : (double) wins / gamesPlayed * 100;
    }

    public double getPointsPerGame() {
        return gamesPlayed == 0 ? 0.0 : points.doubleValue() / gamesPlayed;
    }

    public double getGoalsPerGame() {
        return gamesPlayed == 0 ? 0.0 : (double) goalsFor / gamesPlayed;
    }

    public String getForm() {
        // This could be enhanced to track recent form
        if (gamesPlayed == 0) return "N/A";
        double winRate = getWinPercentage();
        if (winRate >= 75) return "Excellent";
        if (winRate >= 50) return "Good";
        if (winRate >= 25) return "Fair";
        return "Poor";
    }

    // Comparison methods for sorting standings
    public int compareByPoints(TournamentStanding other) {
        // Primary: Points (descending)
        int pointsComparison = other.points.compareTo(this.points);
        if (pointsComparison != 0) return pointsComparison;

        // Secondary: Goal difference (descending)
        int goalDiffComparison = Integer.compare(other.goalDifference, this.goalDifference);
        if (goalDiffComparison != 0) return goalDiffComparison;

        // Tertiary: Goals for (descending)
        int goalsForComparison = Integer.compare(other.goalsFor, this.goalsFor);
        if (goalsForComparison != 0) return goalsForComparison;

        // Quaternary: Games played (ascending - fewer games played is better if points are equal)
        int gamesComparison = Integer.compare(this.gamesPlayed, other.gamesPlayed);
        if (gamesComparison != 0) return gamesComparison;

        // Final: Team name (alphabetical)
        return this.registration.getDisplayName().compareTo(other.registration.getDisplayName());
    }

    public int compareByWins(TournamentStanding other) {
        // Primary: Wins (descending)
        int winsComparison = Integer.compare(other.wins, this.wins);
        if (winsComparison != 0) return winsComparison;

        // Secondary: Goal difference (descending)
        int goalDiffComparison = Integer.compare(other.goalDifference, this.goalDifference);
        if (goalDiffComparison != 0) return goalDiffComparison;

        // Tertiary: Goals for (descending)
        return Integer.compare(other.goalsFor, this.goalsFor);
    }

    // Display methods
    public String getSummary() {
        return String.format(
                "P:%d W:%d L:%d D:%d GF:%d GA:%d GD:%d Pts:%.1f",
                gamesPlayed,
                wins,
                losses,
                draws,
                goalsFor,
                goalsAgainst,
                goalDifference,
                points.doubleValue());
    }

    public String getDisplayName() {
        return registration.getDisplayName();
    }
}
