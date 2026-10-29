package br.com.muxe.core.bukkit.shop.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LoadingRingTest {

    @Test
    void theLitPaneGoesClockwiseAroundTheCenter() {
        assertThat(LoadingRing.litSlot(0)).isEqualTo(3);
        assertThat(LoadingRing.litSlot(1)).isEqualTo(4);
        assertThat(LoadingRing.litSlot(2)).isEqualTo(5);
        assertThat(LoadingRing.litSlot(3)).isEqualTo(14);
        assertThat(LoadingRing.litSlot(4)).isEqualTo(23);
        assertThat(LoadingRing.litSlot(7)).isEqualTo(12);
        assertThat(LoadingRing.litSlot(8)).isEqualTo(3);
    }

    @Test
    void theRingSurroundsTheMiddleSlot() {
        assertThat(LoadingRing.SLOTS).containsExactlyInAnyOrder(3, 4, 5, 12, 14, 21, 22, 23).doesNotContain(13);
    }

    @Test
    void theAnimationRunsForTheWholeDuration() {
        assertThat(LoadingRing.steps(60, 3)).isEqualTo(20);
        assertThat(LoadingRing.steps(1, 3)).isEqualTo(1);
        assertThatThrownBy(() -> LoadingRing.steps(60, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
