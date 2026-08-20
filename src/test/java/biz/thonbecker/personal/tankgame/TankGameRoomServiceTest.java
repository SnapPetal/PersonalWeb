package biz.thonbecker.personal.tankgame;

import static org.assertj.core.api.Assertions.assertThat;

import biz.thonbecker.personal.tankgame.application.TankGameRoomService;
import biz.thonbecker.personal.tankgame.domain.GameState;
import biz.thonbecker.personal.tankgame.domain.GameStateSnapshot;
import biz.thonbecker.personal.tankgame.domain.PlayerInput;
import biz.thonbecker.personal.tankgame.domain.Tank;
import org.junit.jupiter.api.Test;

class TankGameRoomServiceTest {

    private final TankGameRoomService rooms = new TankGameRoomService();

    @Test
    void startsAWaitingGameWhenTheSecondTankJoins() {
        final var game = rooms.findOrCreateWaitingGame();
        rooms.joinGame(game.getGameId(), "Pilot One");
        assertThat(game.getStatus()).isEqualTo(GameState.GameStatus.WAITING);

        rooms.joinGame(game.getGameId(), "Pilot Two");
        assertThat(game.getStatus()).isEqualTo(GameState.GameStatus.PLAYING);
    }

    @Test
    void sanitizesInputAndIgnoresUnknownTanks() {
        final var game = rooms.createGame();
        final Tank tank = rooms.joinGame(game.getGameId(), "Pilot");
        final var input = new PlayerInput();
        input.setMouseX(Double.POSITIVE_INFINITY);
        input.setMouseY(-100);
        input.setShoot(true);

        rooms.updateInput(tank.getId(), input);
        final var stored = rooms.getInput(tank.getId());
        assertThat(stored.getMouseX()).isEqualTo(game.getMapWidth() / 2.0);
        assertThat(stored.getMouseY()).isZero();
        assertThat(stored.isShoot()).isTrue();

        rooms.updateInput("missing", input);
        assertThat(rooms.getInput("missing")).isNull();
    }

    @Test
    void snapshotContainsOnlyClientState() {
        final var game = rooms.createGame();
        rooms.joinGame(game.getGameId(), "Pilot");

        final var snapshot = GameStateSnapshot.from(game);
        assertThat(snapshot.tanks()).hasSize(1);
        assertThat(snapshot.tanks().values().iterator().next().playerName()).isEqualTo("Pilot");
        assertThat(snapshot.walls()).isNotEmpty();
    }
}
