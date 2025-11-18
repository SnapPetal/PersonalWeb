package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;

public interface GameWithPlayers {
    Long getId();

    Integer getWhiteTeamScore();

    Integer getBlackTeamScore();

    Game.TeamColor getWinner();

    LocalDateTime getPlayedAt();

    String getNotes();

    @Value("#{target.whiteTeamPlayer1.name}")
    String getWhiteTeamPlayer1Name();

    @Value("#{target.whiteTeamPlayer2.name}")
    String getWhiteTeamPlayer2Name();

    @Value("#{target.blackTeamPlayer1.name}")
    String getBlackTeamPlayer1Name();

    @Value("#{target.blackTeamPlayer2.name}")
    String getBlackTeamPlayer2Name();
}
