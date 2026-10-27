package br.com.muxe.core.bukkit.menu.button;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfiguredButtonTest {

    private static final String BUTTONS = String.join("\n",
        "buttons:",
        "  bedwars:",
        "    slot: 10",
        "    game: BedWars",
        "    item:",
        "      material: BED",
        "      name: \"&b&lBED WARS\"",
        "  custom-room:",
        "    slot: 35",
        "    shop: ultra-plus",
        "    item:",
        "      material: BREWING_STAND_ITEM",
        "  friends:",
        "    slot: 29",
        "    item:",
        "      material: COOKIE",
        "");

    @Test
    void eachButtonReadsItsSlotItemAndAction() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(BUTTONS);

        List<ConfiguredButton> buttons = ConfiguredButton.list(yaml.getConfigurationSection("buttons"));

        assertThat(buttons).extracting(ConfiguredButton::slot).containsExactly(10, 35, 29);
        assertThat(buttons).extracting(ConfiguredButton::action).containsExactly(ButtonAction.game("bedwars"), ButtonAction.shop("ultra-plus"), ButtonAction.NONE);
        assertThat(buttons.getFirst().displayName()).isEqualTo("BED WARS");
        assertThat(buttons.getLast().displayName()).isEqualTo("friends");
    }

    @Test
    void buttonsMustFitTheMenuAndNotShareASlot() {
        ConfiguredButton first = new ConfiguredButton("a", 10, ItemBuilder.of(Material.STONE), ButtonAction.NONE);
        ConfiguredButton same = new ConfiguredButton("b", 10, ItemBuilder.of(Material.STONE), ButtonAction.NONE);
        ConfiguredButton outside = new ConfiguredButton("c", 36, ItemBuilder.of(Material.STONE), ButtonAction.NONE);

        assertThatThrownBy(() -> ConfiguredButton.requireFit(List.of(first, same), 4)).hasMessageContaining("slot 10");
        assertThatThrownBy(() -> ConfiguredButton.requireFit(List.of(outside), 4)).hasMessageContaining("outside");
        assertThatThrownBy(() -> ButtonAction.game(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
