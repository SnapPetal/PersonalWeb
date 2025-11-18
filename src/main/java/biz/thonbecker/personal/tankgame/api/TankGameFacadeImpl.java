package biz.thonbecker.personal.tankgame.api;

import biz.thonbecker.personal.tankgame.application.TankGameService;
import biz.thonbecker.personal.tankgame.domain.GameState;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TankGameFacadeImpl implements TankGameFacade {

    private final TankGameService tankGameService;

    @Override
    public GameState createGame() {
        return tankGameService.createGame();
    }

    @Override
    public GameState getGame(String gameId) {
        return tankGameService.getGame(gameId);
    }

    @Override
    public Map<String, GameState> getActiveGames() {
        return tankGameService.getActiveGames();
    }
}
