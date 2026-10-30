package br.com.muxe.minigame.engine.match;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.engine.match.StartCountdown.Change;
import br.com.muxe.minigame.engine.mode.GameMode;
import org.junit.jupiter.api.Test;

class StartCountdownTest {

    private final GameMode solo = new GameMode("solo", "Solo", 6, 8, 1);
    private final GameMode oneVsOne = new GameMode("1v1", "1v1", 2, 2, 1);

    @Test
    void belowTheMinimumTheRoomKeepsWaiting() {
        StartCountdown countdown = new StartCountdown(solo);

        assertThat(countdown.players(5)).isEqualTo(Change.NONE);
        assertThat(countdown.remaining()).isEmpty();
    }

    @Test
    void theMinimumStartsThirtySecondsAndAFullRoomCutsToTen() {
        StartCountdown countdown = new StartCountdown(solo);

        assertThat(countdown.players(6)).isEqualTo(Change.STARTED);
        assertThat(countdown.remaining()).hasValue(30);
        assertThat(countdown.players(7)).isEqualTo(Change.NONE);
        assertThat(countdown.players(8)).isEqualTo(Change.SHORTENED);
        assertThat(countdown.remaining()).hasValue(10);
    }

    @Test
    void aFullRoomNeverAddsTimeBack() {
        StartCountdown countdown = new StartCountdown(solo);
        countdown.players(6);
        for (int second = 0; second < 25; second++) {
            countdown.tick();
        }

        assertThat(countdown.players(8)).isEqualTo(Change.NONE);
        assertThat(countdown.remaining()).hasValue(5);
    }

    @Test
    void droppingBelowTheMinimumCancelsAndReachingItAgainStartsOver() {
        StartCountdown countdown = new StartCountdown(solo);
        countdown.players(6);
        countdown.tick();

        assertThat(countdown.players(5)).isEqualTo(Change.CANCELLED);
        assertThat(countdown.isRunning()).isFalse();
        assertThat(countdown.players(6)).isEqualTo(Change.STARTED);
        assertThat(countdown.remaining()).hasValue(30);
    }

    @Test
    void modesWhereTheMinimumIsTheMaximumStartAtTen() {
        StartCountdown countdown = new StartCountdown(oneVsOne);

        assertThat(countdown.players(2)).isEqualTo(Change.STARTED);
        assertThat(countdown.remaining()).hasValue(10);
    }

    @Test
    void theCountdownFinishesAfterItsLastSecond() {
        StartCountdown countdown = new StartCountdown(oneVsOne);
        countdown.players(2);
        for (int second = 0; second < 9; second++) {
            assertThat(countdown.tick().finished()).isFalse();
        }

        assertThat(countdown.tick().finished()).isTrue();
        assertThat(countdown.isRunning()).isFalse();
    }
}
