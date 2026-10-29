package br.com.muxe.core.bukkit.bossbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class BossBarFramesTest {

    private final BossBarFrames frames = new BossBarFrames(List.of("&e&lMUXE", "&6&lMUXE"), 10);

    @Test
    void framesAlternateEveryPeriod() {
        assertThat(frames.at(0)).isEqualTo("§e§lMUXE");
        assertThat(frames.at(9)).isEqualTo("§e§lMUXE");
        assertThat(frames.at(10)).isEqualTo("§6§lMUXE");
        assertThat(frames.at(20)).isEqualTo("§e§lMUXE");
    }

    @Test
    void aFrameListIsNeverEmpty() {
        assertThatThrownBy(() -> new BossBarFrames(List.of(), 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theCarrierIsPlacedAlongTheLookDirection() {
        Location eyes = new Location(null, 0, 64, 0, 0f, 0f);

        Location carrier = BossBarPlacement.inFront(eyes, BossBarPlacement.DISTANCE);

        assertThat(carrier.getZ()).isCloseTo(BossBarPlacement.DISTANCE, within(1e-9));
        assertThat(carrier.getX()).isCloseTo(0, within(1e-9));
        assertThat(carrier.getY()).isCloseTo(64, within(1e-9));
    }

    @Test
    void smallMovementsDoNotMoveTheCarrier() {
        Location before = new Location(null, 0, 64, 0, 0f, 0f);
        Location after = new Location(null, 0.1, 64, 0, 1f, 0f);
        Location turned = new Location(null, 0, 64, 0, 30f, 0f);
        Location walked = new Location(null, 2, 64, 0, 0f, 0f);

        assertThat(BossBarPlacement.needsMove(before, after)).isFalse();
        assertThat(BossBarPlacement.needsMove(before, turned)).isTrue();
        assertThat(BossBarPlacement.needsMove(before, walked)).isTrue();
    }
}
