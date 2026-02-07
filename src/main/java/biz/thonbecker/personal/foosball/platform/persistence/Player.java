package biz.thonbecker.personal.foosball.platform.persistence;

import static biz.thonbecker.personal.foosball.platform.RatingService.INITIAL_RATING;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@ToString(
        exclude = {"whiteTeamPlayer1Games", "whiteTeamPlayer2Games", "blackTeamPlayer1Games", "blackTeamPlayer2Games"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(name = "players", schema = "foosball")
@EntityListeners(AuditingEntityListener.class)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Player name is required")
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    // Rating system fields
    @Column(name = "rating", nullable = false)
    private Integer rating = INITIAL_RATING;

    @Column(name = "peak_rating", nullable = false)
    private Integer peakRating = INITIAL_RATING;

    @Column(name = "games_played", nullable = false)
    private Integer gamesPlayed = 0;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    private Integer bestStreak = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Relationships
    @OneToMany(mappedBy = "whiteTeamPlayer1", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Game> whiteTeamPlayer1Games = new ArrayList<>();

    @OneToMany(mappedBy = "whiteTeamPlayer2", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Game> whiteTeamPlayer2Games = new ArrayList<>();

    @OneToMany(mappedBy = "blackTeamPlayer1", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Game> blackTeamPlayer1Games = new ArrayList<>();

    @OneToMany(mappedBy = "blackTeamPlayer2", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Game> blackTeamPlayer2Games = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }
}
