package biz.thonbecker.personal.foosball.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoosballPlayer {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("whiteTeamPlayer1Games")
    private List<FoosballGame> whiteTeamPlayer1Games;

    @JsonProperty("whiteTeamPlayer2Games")
    private List<FoosballGame> whiteTeamPlayer2Games;

    @JsonProperty("blackTeamPlayer1Games")
    private List<FoosballGame> blackTeamPlayer1Games;

    @JsonProperty("blackTeamPlayer2Games")
    private List<FoosballGame> blackTeamPlayer2Games;
}
