package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FallCapTest {

    @Test
    void aFallAfterAnItemLaunchTakesAtMostOneAndAHalfHearts() {
        assertThat(FallCap.damage(17, 1_000, 3_000)).isEqualTo(3.0);
        assertThat(FallCap.damage(2, 1_000, 3_000)).isEqualTo(2.0);
        assertThat(FallCap.MAX_DAMAGE).isEqualTo(3.0);
    }

    @Test
    void anOldLaunchNoLongerSoftensTheFall() {
        assertThat(FallCap.damage(17, 1_000, 1_000 + FallCap.LIMIT_MILLIS + 1)).isEqualTo(17.0);
    }

    @Test
    void thePlayerHasLandedOnlyOnTheGroundAfterTheLaunchHadTimeToLiftThem() {
        assertThat(FallCap.landed(1_000, 1_200, true)).isFalse();
        assertThat(FallCap.landed(1_000, 1_600, false)).isFalse();
        assertThat(FallCap.landed(1_000, 1_600, true)).isTrue();
    }
}
