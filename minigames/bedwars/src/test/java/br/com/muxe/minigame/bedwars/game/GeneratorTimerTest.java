package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import org.junit.jupiter.api.Test;

class GeneratorTimerTest {

    @Test
    void aDelayBelowOneSecondStillDropsEveryFourCyclesLikeTheLegacyForge() {
        GeneratorTimer iron = new GeneratorTimer(new Rate(0.9, 2, 48));

        int dropped = 0;
        int cycles = 0;
        while (dropped == 0) {
            dropped = iron.cycle(0);
            cycles++;
        }

        assertThat(cycles).isEqualTo(5);
        assertThat(dropped).isEqualTo(2);
        assertThat(cyclesUntilDrop(iron)).isEqualTo(4);
        assertThat(cyclesUntilDrop(iron)).isEqualTo(4);
    }

    @Test
    void theFirstDropComesAfterTheDelay() {
        GeneratorTimer diamond = new GeneratorTimer(new Rate(30, 1, 4));

        assertThat(cyclesUntilDrop(diamond)).isEqualTo(121);
        assertThat(cyclesUntilDrop(diamond)).isEqualTo(120);
    }

    @Test
    void theSpawnLimitCountsWhatIsAlreadyOnTheGroundAndAllowsAPartialDrop() {
        GeneratorTimer iron = new GeneratorTimer(new Rate(1, 4, 48));
        cyclesUntilDrop(iron);

        drainUntilDue(iron);
        assertThat(iron.cycle(46)).isEqualTo(2);
        drainUntilDue(iron);
        assertThat(iron.cycle(48)).isZero();
    }

    @Test
    void aNewTierChangesTheRateWithoutRestartingTheCountdown() {
        GeneratorTimer diamond = new GeneratorTimer(new Rate(30, 1, 4));
        for (int cycle = 0; cycle < 100; cycle++) {
            diamond.cycle(0);
        }
        int before = diamond.secondsLeft();

        diamond.apply(new Rate(20, 1, 4));

        assertThat(diamond.secondsLeft()).isEqualTo(before).isEqualTo(5);
        assertThat(cyclesUntilDrop(diamond)).isEqualTo(21);
        assertThat(cyclesUntilDrop(diamond)).isEqualTo(80);
    }

    @Test
    void theHologramShowsWholeSecondsRoundedUp() {
        GeneratorTimer emerald = new GeneratorTimer(new Rate(60, 1, 4));

        assertThat(emerald.secondsLeft()).isEqualTo(60);
        emerald.cycle(0);
        assertThat(emerald.secondsLeft()).isEqualTo(60);
        for (int cycle = 0; cycle < 4; cycle++) {
            emerald.cycle(0);
        }
        assertThat(emerald.secondsLeft()).isEqualTo(59);
    }

    private static int cyclesUntilDrop(GeneratorTimer timer) {
        int cycles = 0;
        int dropped = 0;
        while (dropped == 0) {
            dropped = timer.cycle(0);
            cycles++;
        }
        return cycles;
    }

    private static void drainUntilDue(GeneratorTimer timer) {
        while (timer.secondsLeft() > 0) {
            timer.cycle(0);
        }
    }
}
