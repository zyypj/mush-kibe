package br.com.muxe.core.bukkit.customroom;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.common.customroom.schema.RoomTexts;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

public record CustomRoomSettings(ConfigurationSection root, RoomTexts texts) {

    public CustomRoomSettings {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(texts, "texts");
    }

    public static CustomRoomSettings from(ConfigurationSection section) {
        ConfigurationSection root = section == null ? new MemoryConfiguration() : section;
        Map<String, String> texts = new HashMap<>();
        ConfigurationSection textSection = root.getConfigurationSection("texts");
        if (textSection != null) {
            textSection.getKeys(false).forEach(key -> texts.put(key, textSection.getString(key)));
        }
        return new CustomRoomSettings(root, new RoomTexts(texts));
    }

    public ConfigurationSection section(String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        return section == null ? new MemoryConfiguration() : section;
    }

    public ItemBuilder item(String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        return section == null || !section.contains("material") ? ItemBuilder.of(Material.STONE) : ItemBuilder.from(section);
    }

    public int slot(String path) {
        return root.getInt(path + ".slot", -1);
    }

    public List<Integer> slots(String path) {
        return root.getIntegerList(path);
    }

    public String text(String path, String fallback) {
        return root.getString(path, fallback);
    }

    public String codeSuffix() {
        return root.getString("code-suffix", " (C)");
    }
}
