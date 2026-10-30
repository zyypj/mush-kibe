package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record MenuSlot(int slot, ItemBuilder item) {

    public static MenuSlot from(ConfigurationSection section) {
        return new MenuSlot(section.getInt("slot"), ItemBuilder.from(Objects.requireNonNull(section.getConfigurationSection("item"), "Missing item at " + section.getCurrentPath())));
    }
}
