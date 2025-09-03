package solutions.thonbecker.personal.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FoosballStats {
    @JsonProperty("name")
    private String playerName;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("totalGames")
    private Integer gamesPlayed;

    @JsonProperty("wins")
    private Integer wins;

    @JsonProperty("losses")
    private Integer losses;

    @JsonProperty("winPercentage")
    private Double winPercentage;

    @JsonProperty("formattedWinPercentage")
    private String formattedWinPercentage;

    // Legacy fields for backward compatibility
    private Integer goalsScored;
    private Integer goalsConceded;
    private String position;
}
