package br.com.muxe.core.bukkit.menu.button;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.MenuLayout;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public record ConfiguredButton(String id, int slot, ItemBuilder item, ButtonAction action) {

    public ConfiguredButton {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(action, "action");
        if (slot < 0) {
            throw new IllegalArgumentException("Button '" + id + "' has a negative slot");
        }
    }

    public static ConfiguredButton from(ConfigurationSection section) {
        ConfigurationSection item = Objects.requireNonNull(section.getConfigurationSection("item"), "Missing configuration section " + section.getCurrentPath() + ".item");
        return new ConfiguredButton(section.getName(), section.getInt("slot", -1), ItemBuilder.from(item), ButtonAction.from(section));
    }

    public static List<ConfiguredButton> list(ConfigurationSection section) {
        List<ConfiguredButton> buttons = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                buttons.add(from(Objects.requireNonNull(section.getConfigurationSection(key), section.getCurrentPath() + "." + key)));
            }
        }
        return List.copyOf(buttons);
    }

    public static void requireFit(List<ConfiguredButton> buttons, int rows) {
        Set<Integer> taken = new HashSet<>();
        for (ConfiguredButton button : buttons) {
            if (button.slot() >= rows * MenuLayout.ROW_WIDTH) {
                throw new IllegalArgumentException("Button '" + button.id() + "' is at slot " + button.slot() + ", outside a menu of " + rows + " rows");
            }
            if (!taken.add(button.slot())) {
                throw new IllegalArgumentException("Two buttons share slot " + button.slot());
            }
        }
    }

    public String displayName() {
        return item.name() == null ? id : Colors.strip(item.name());
    }
}
