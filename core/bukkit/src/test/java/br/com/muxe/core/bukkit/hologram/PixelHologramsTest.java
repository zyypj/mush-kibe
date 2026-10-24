package br.com.muxe.core.bukkit.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.effect.PixelFont;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class PixelHologramsTest {

    private static final double SIZE = 0.5;

    private final RecordingTransport transport = new RecordingTransport();
    private final PixelHolograms holograms = new PixelHolograms(PixelFont.standard(), SIZE, 2, HologramStyle.standard(), transport);

    @Test
    void theTextIsCenteredOnTheAnchorAndGrowsUpwards() {
        Location anchor = new Location(null, 10, 64, 10, 0f, 0f);

        List<HologramPixel> pixels = holograms.layout(anchor, "1");

        double minX = pixels.stream().mapToDouble(pixel -> pixel.location().getX()).min().orElseThrow();
        double maxX = pixels.stream().mapToDouble(pixel -> pixel.location().getX()).max().orElseThrow();
        assertThat((minX + maxX) / 2).isCloseTo(10, within(1e-9));
        assertThat(pixels).allMatch(pixel -> pixel.location().getZ() == 10);
        assertThat(pixels.stream().mapToDouble(pixel -> pixel.location().getY()).min().orElseThrow()).isCloseTo(64, within(1e-9));
        assertThat(pixels.stream().mapToDouble(pixel -> pixel.location().getY()).max().orElseThrow()).isCloseTo(64 + 6 * SIZE, within(1e-9));
    }

    @Test
    void textReadsFromLeftToRightForTheViewer() {
        Location facingSouth = new Location(null, 0, 0, 0, 0f, 0f);

        List<HologramPixel> pixels = holograms.layout(facingSouth, "1 ");

        assertThat(pixels).allMatch(pixel -> pixel.location().getX() > 0);
    }

    @Test
    void textTurnsWithTheAnchor() {
        Location facingEast = new Location(null, 0, 0, 0, 270f, 0f);

        List<HologramPixel> pixels = holograms.layout(facingEast, "1 ");

        assertThat(pixels).allMatch(pixel -> Math.abs(pixel.location().getX()) < 1e-9);
        assertThat(pixels).allMatch(pixel -> pixel.location().getZ() < 0);
    }

    @Test
    void pixelsAreColoredByShade() {
        List<HologramPixel> pixels = holograms.layout(new Location(null, 0, 0, 0), "8");

        assertThat(pixels).extracting(HologramPixel::name).containsOnly("§f⬛", "§a⬛", "§2⬛");
        assertThat(pixels).filteredOn(pixel -> pixel.name().equals("§f⬛")).hasSize(3);
    }

    @Test
    void heightCoversEveryRow() {
        assertThat(holograms.height()).isCloseTo(PixelFont.HEIGHT * SIZE, within(1e-9));
    }

    @Test
    void showSendsTheLayoutToTheViewer() {
        Location anchor = new Location(null, 0, 0, 0);

        holograms.show(null, anchor, "7");

        assertThat(transport.sent).hasSize(1);
        assertThat(transport.sent.getFirst()).hasSameSizeAs(holograms.layout(anchor, "7"));
    }

    private static final class RecordingTransport implements HologramTransport {

        private final List<List<HologramPixel>> sent = new ArrayList<>();

        @Override
        public SpawnedHologram spawn(Player viewer, List<HologramPixel> pixels) {
            sent.add(pixels);
            return SpawnedHologram.NONE;
        }
    }
}
