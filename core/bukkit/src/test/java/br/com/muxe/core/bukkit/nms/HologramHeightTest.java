package br.com.muxe.core.bukkit.nms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.npc.NpcLabelLayout;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class HologramHeightTest {

    private static final double REFERENCE_FEET = 33.375;
    private static final double REFERENCE_NAME_TAG = PacketHologramTransport.CLIENT_STAND_HEIGHT + PacketHologramTransport.NAME_TAG_OFFSET;

    @Test
    void theTextOfALineStartsJustAboveItsLabelOnTheOneEightClient() {
        for (double label : List.of(0.0, 2.0, 35.5)) {
            double textTop = PacketHologramTransport.clientTextTop(PacketHologramTransport.standY(label));

            assertThat(textTop - label).isCloseTo(PacketHologramTransport.TEXT_TOP_ABOVE_LABEL, within(1e-9));
        }
    }

    @Test
    void gameNpcLinesLandWhereTheReferenceStandsShowedTheirNames() {
        NpcLabelLayout layout = NpcLabelLayout.from(ShopFixtures.CONFIG.getConfigurationSection("npcs.labels"));
        List<DisplayLabel> labels = layout.place(List.of("&d&lNOVIDADE!", "&b&lDUELS", "&e177 jogando"));
        List<Double> referenceStands = List.of(34.0, 33.53125, 33.25);

        for (int index = 0; index < labels.size(); index++) {
            double ours = PacketHologramTransport.clientTextTop(PacketHologramTransport.standY(REFERENCE_FEET + labels.get(index).height()));
            double reference = referenceStands.get(index) + REFERENCE_NAME_TAG;

            assertThat(ours).as(labels.get(index).text()).isCloseTo(reference, within(1e-6));
        }
    }
}
