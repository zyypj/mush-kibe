package br.com.muxe.core.bukkit.display;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.hologram.HologramPixel;
import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class DisplayTest {

    private final Location anchor = new Location(null, 7.5, 33, -9.5, 180, 0);

    @Test
    void stillStandsKeepTheirOffsetAndYaw() {
        DisplayStand stand = stand(0.19, -0.03, -0.81, 45, StandMotion.NONE);

        Location at = stand.at(anchor, 200);

        assertThat(at.getX()).isCloseTo(7.69, within(1e-9));
        assertThat(at.getY()).isCloseTo(32.97, within(1e-9));
        assertThat(at.getZ()).isCloseTo(-10.31, within(1e-9));
        assertThat(at.getYaw()).isEqualTo(45f);
    }

    @Test
    void spinningStandsTurnEveryTickAndWrapAround() {
        StandMotion motion = StandMotion.spinning(10);

        assertThat(motion.yaw(350, 0)).isEqualTo(350f);
        assertThat(motion.yaw(350, 2)).isEqualTo(10f);
        assertThat(StandMotion.spinning(-10).yaw(5, 1)).isEqualTo(355f);
    }

    @Test
    void bobbingFollowsASineWaveWithItsPhase() {
        StandMotion motion = new StandMotion(0, 0.1, 40, 0);
        StandMotion opposite = new StandMotion(0, 0.1, 40, 180);

        assertThat(motion.lift(0)).isCloseTo(0, within(1e-9));
        assertThat(motion.lift(10)).isCloseTo(0.1, within(1e-9));
        assertThat(opposite.lift(10)).isCloseTo(-0.1, within(1e-9));
        assertThat(motion.lift(40)).isCloseTo(0, within(1e-9));
    }

    @Test
    void onlySpinningOrBobbingStandsMove() {
        assertThat(StandMotion.NONE.moves()).isFalse();
        assertThat(StandMotion.spinning(3).moves()).isTrue();
        assertThat(new StandMotion(0, 0.1, 20, 0).moves()).isTrue();
    }

    @Test
    void bobbingNeedsAPositivePeriod() {
        assertThatThrownBy(() -> new StandMotion(0, 0.1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StandMotion(0, -1, 20, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void labelsSitAboveTheAnchorAndSkipBlankLines() {
        Display display = new Display("shop", anchor, List.of(), List.of(
            new DisplayLabel("&b&lLOJA", 3.6),
            new DisplayLabel("", 3.0),
            new DisplayLabel("&eClique para comprar!", 2.25)
        ));

        List<HologramPixel> pixels = display.labelPixels();

        assertThat(pixels).hasSize(2);
        assertThat(pixels.get(0).name()).isEqualTo("§b§lLOJA");
        assertThat(pixels.get(0).location().getY()).isCloseTo(36.6, within(1e-9));
        assertThat(pixels.get(1).location().getY()).isCloseTo(35.25, within(1e-9));
    }

    @Test
    void labelsCanBeFilledInForEachViewer() {
        Display display = new Display("parkour", anchor, List.of(), List.of(
            new DisplayLabel("&b&lPARKOUR", 2.075),
            new DisplayLabel("&eRecorde pessoal: &7{record}", 1.2625)
        ));

        List<HologramPixel> pixels = display.labelPixels(text -> text.replace("{record}", "3m 14,002s"));

        assertThat(pixels).extracting(HologramPixel::name).containsExactly("§b§lPARKOUR", "§eRecorde pessoal: §73m 14,002s");
        assertThat(display.labelPixels(text -> text.startsWith("&e") ? "" : text)).hasSize(1);
    }

    private static DisplayStand stand(double x, double y, double z, float yaw, StandMotion motion) {
        return new DisplayStand(x, y, z, yaw, true, false, Optional.empty(), Optional.empty(), StandPose.ZERO, StandPose.ZERO, motion);
    }
}
