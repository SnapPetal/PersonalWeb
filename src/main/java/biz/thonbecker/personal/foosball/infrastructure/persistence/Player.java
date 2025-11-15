package biz.thonbecker.personal.foosball.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(
        exclude = {
            "whiteTeamPlayer1Games",
            "whiteTeamPlayer2Games",
            "blackTeamPlayer1Games",
            "blackTeamPlayer2Games"
        })
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

    @Email(message = "Email should be valid")
    @Column(name = "email", length = 255)
    private String email;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public Player(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
