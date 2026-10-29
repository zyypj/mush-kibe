package br.com.muxe.core.bukkit.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class TextHologramsTest {

    private final TextHolograms holograms = new TextHolograms((viewer, labels) -> SpawnedHologram.NONE, 0.3);

    @Test
    void linesGoDownFromTheTop() {
        List<HologramPixel> labels = holograms.layout(new Location(null, 1, 70, 1), List.of("&b&lSKYWARS", "&e10 jogando"));

        assertThat(labels).extracting(HologramPixel::name).containsExactly("§b§lSKYWARS", "§e10 jogando");
        assertThat(labels.get(0).location().getY()).isCloseTo(70, within(1e-9));
        assertThat(labels.get(1).location().getY()).isCloseTo(69.7, within(1e-9));
    }

    @Test
    void blankLinesKeepTheirSpaceButShowNothing() {
        List<HologramPixel> labels = holograms.layout(new Location(null, 0, 10, 0), List.of("a", "", "b"));

        assertThat(labels).extracting(HologramPixel::name).containsExactly("a", "b");
        assertThat(labels.get(1).location().getY()).isCloseTo(9.4, within(1e-9));
    }

    @Test
    void theHeightCoversEveryLine() {
        assertThat(holograms.height(3)).isCloseTo(0.6, within(1e-9));
        assertThat(holograms.height(0)).isZero();
    }
}
