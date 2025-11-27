package biz.thonbecker.personal.foosball.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@ToString(exclude = {"whiteTeamPlayer1", "whiteTeamPlayer2", "blackTeamPlayer1", "blackTeamPlayer2"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(name = "games", schema = "foosball")
@EntityListeners(AuditingEntityListener.class)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "White team player 1 is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_team_player1_id", nullable = false)
    @JsonBackReference
    private Player whiteTeamPlayer1;

    @NotNull(message = "White team player 2 is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_team_player2_id", nullable = false)
    @JsonBackReference
    private Player whiteTeamPlayer2;

    @NotNull(message = "Black team player 1 is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_team_player1_id", nullable = false)
    @JsonBackReference
    private Player blackTeamPlayer1;

    @NotNull(message = "Black team player 2 is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_team_player2_id", nullable = false)
    @JsonBackReference
    private Player blackTeamPlayer2;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner", length = 10)
    private TeamColor winner;

    @CreatedDate
    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    public enum TeamColor {
        WHITE,
        BLACK
    }

    public Game(Player whiteTeamPlayer1, Player whiteTeamPlayer2, Player blackTeamPlayer1, Player blackTeamPlayer2) {
        this.whiteTeamPlayer1 = whiteTeamPlayer1;
        this.whiteTeamPlayer2 = whiteTeamPlayer2;
        this.blackTeamPlayer1 = blackTeamPlayer1;
        this.blackTeamPlayer2 = blackTeamPlayer2;
    }

    // Business logic methods
    public void setWinner(TeamColor winner) {
        this.winner = winner;
    }

    public boolean isDraw() {
        return winner == null;
    }

    public boolean isWhiteTeamWinner() {
        return TeamColor.WHITE.equals(winner);
    }

    public boolean isBlackTeamWinner() {
        return TeamColor.BLACK.equals(winner);
    }
}
