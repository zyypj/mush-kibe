package br.com.muxe.core.bukkit.parkour;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.effect.FireworkBurst;
import br.com.muxe.core.bukkit.hotbar.HotbarItem;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.placement.domain.ParkourPoint;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ParkourSettingsTest {

    private final ParkourSettings settings = ParkourSettings.from(ShopFixtures.CONFIG.getConfigurationSection("parkour"));

    @Test
    void theParkourItemsGoInSlotsFourFiveAndSix() {
        assertThat(settings.items().get(ParkourItem.RESTART)).isEqualTo(new HotbarItem(3, settings.items().get(ParkourItem.RESTART).item()));
        assertThat(settings.items().get(ParkourItem.RESTART).item().material()).isEqualTo(Material.RECORD_9);
        assertThat(settings.items().get(ParkourItem.CHECKPOINT).slot()).isEqualTo(4);
        assertThat(settings.items().get(ParkourItem.CHECKPOINT).item().material()).isEqualTo(Material.GOLD_PLATE);
        assertThat(settings.items().get(ParkourItem.STOP).slot()).isEqualTo(5);
        assertThat(settings.items().get(ParkourItem.STOP).item().material()).isEqualTo(Material.REDSTONE);
    }

    @Test
    void anItemIsRecognisedByItsSlotAndMaterial() {
        assertThat(settings.itemAt(4, new ItemStack(Material.GOLD_PLATE))).contains(ParkourItem.CHECKPOINT);
        assertThat(settings.itemAt(4, new ItemStack(Material.CHEST))).isEmpty();
        assertThat(settings.itemAt(5, new ItemStack(Material.GOLD_PLATE))).isEmpty();
        assertThat(settings.itemAt(3, null)).isEmpty();
    }

    @Test
    void hologramsFollowTheExportedLayout() {
        assertThat(settings.labelsFor(ParkourPoint.start())).containsExactly(
            new DisplayLabel("&b&lPARKOUR", 2.075),
            new DisplayLabel("&aInício", 1.79375),
            new DisplayLabel("&eRecorde pessoal: &7{record}", 1.2625)
        );
        assertThat(settings.labelsFor(ParkourPoint.checkpoint(2))).containsExactly(
            new DisplayLabel("&b&lPARKOUR", 1.54375),
            new DisplayLabel("&eCheckpoint &a#2", 1.2625)
        );
        assertThat(settings.labelsFor(ParkourPoint.end())).extracting(DisplayLabel::text).containsExactly("&b&lPARKOUR", "&cFim");
        assertThat(settings.noRecord()).isEqualTo("...");
    }

    @Test
    void theFinishBurstIsGreenCyanAndRed() {
        FireworkEffect firework = settings.finishFirework();

        assertThat(firework.getColors()).containsExactly(FireworkBurst.color("#55FF55"), FireworkBurst.color("#55FFFF"), FireworkBurst.color("#FF5555"));
        assertThat(firework.getType()).isEqualTo(FireworkEffect.Type.BALL_LARGE);
    }

    @Test
    void twoItemsCannotShareASlot() {
        Map<ParkourItem, HotbarItem> items = new EnumMap<>(ParkourItem.class);
        items.put(ParkourItem.RESTART, new HotbarItem(3, ItemBuilder.of(Material.RECORD_9)));
        items.put(ParkourItem.CHECKPOINT, new HotbarItem(3, ItemBuilder.of(Material.GOLD_PLATE)));
        items.put(ParkourItem.STOP, new HotbarItem(5, ItemBuilder.of(Material.REDSTONE)));

        assertThatThrownBy(() -> new ParkourSettings(items, settings.labels(), "...", settings.finishFirework()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
