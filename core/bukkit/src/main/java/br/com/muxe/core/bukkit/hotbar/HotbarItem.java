package br.com.muxe.core.bukkit.hotbar;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record HotbarItem(int slot, ItemBuilder item) {

    public static final int HOTBAR_SIZE = 9;

    public HotbarItem {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("Hotbar slots go from 1 to 9");
        }
        Objects.requireNonNull(item, "item");
    }

    public static HotbarItem from(ConfigurationSection section) {
        return new HotbarItem(slotOf(section), ItemBuilder.from(Objects.requireNonNull(section.getConfigurationSection("item"), section.getCurrentPath() + ".item")));
    }

    public static int slotOf(ConfigurationSection section) {
        return section.getInt("slot") - 1;
    }
}
