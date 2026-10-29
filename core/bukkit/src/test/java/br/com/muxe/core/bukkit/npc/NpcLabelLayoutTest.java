package br.com.muxe.core.bukkit.npc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.hologram.HologramPixel;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class NpcLabelLayoutTest {

    private final NpcLabelLayout layout = NpcLabelLayout.from(ShopFixtures.CONFIG.getConfigurationSection("npcs.labels"));

    @Test
    void theLastLineSitsAtTheBottomAndTheOthersGoUp() {
        assertThat(layout.place(List.of("&b&lHG", "&e2 jogando"))).containsExactly(
            new DisplayLabel("&b&lHG", 2.25625 + 0.28125),
            new DisplayLabel("&e2 jogando", 2.25625)
        );
    }

    @Test
    void aThirdLineAboveTheNameUsesTheWiderGap() {
        assertThat(layout.place(List.of("&d&lNOVIDADE!", "&b&lDUELS", "&e177 jogando"))).containsExactly(
            new DisplayLabel("&d&lNOVIDADE!", 2.25625 + 0.28125 + 0.46875),
            new DisplayLabel("&b&lDUELS", 2.25625 + 0.28125),
            new DisplayLabel("&e177 jogando", 2.25625)
        );
    }

    @Test
    void pixelsStandAboveTheFeetAndSkipBlankLines() {
        Location feet = new Location(null, 2.5, 33.375, 1.5);

        List<HologramPixel> pixels = layout.pixels(feet, List.of("&b&lDUELS", "", "&e0 jogando"));

        assertThat(pixels).extracting(HologramPixel::name).containsExactly("&b&lDUELS", "&e0 jogando");
        assertThat(pixels.get(1).location().getY()).isCloseTo(35.63125, within(1e-9));
        assertThat(pixels.get(0).location().getY()).isCloseTo(36.38125, within(1e-9));
        assertThat(feet.getY()).isEqualTo(33.375);
    }

    @Test
    void withoutConfigTheStandardLayoutKeepsEvenLines() {
        assertThat(NpcLabelLayout.from(null)).isEqualTo(NpcLabelLayout.STANDARD);
        assertThat(NpcLabelLayout.from(new YamlConfiguration())).isEqualTo(NpcLabelLayout.STANDARD);
        assertThat(NpcLabelLayout.STANDARD.place(List.of("a", "b"))).containsExactly(
            new DisplayLabel("a", NpcService.HOLOGRAM_ABOVE_HEAD + 0.28),
            new DisplayLabel("b", NpcService.HOLOGRAM_ABOVE_HEAD)
        );
    }

    @Test
    void spacingAndGapsMustBePositive() {
        assertThatThrownBy(() -> new NpcLabelLayout(2, List.of(), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NpcLabelLayout(2, List.of(-0.1), 0.28)).isInstanceOf(IllegalArgumentException.class);
    }
}
