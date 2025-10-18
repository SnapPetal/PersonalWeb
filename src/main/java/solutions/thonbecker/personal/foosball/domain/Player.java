package solutions.thonbecker.personal.foosball.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Foosball player in the domain model.
 * Players can participate in games and have associated statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String name;

    public Player(String name) {
        this.name = name;
    }
}
