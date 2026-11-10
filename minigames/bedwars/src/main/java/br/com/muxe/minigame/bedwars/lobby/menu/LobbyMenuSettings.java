package br.com.muxe.minigame.bedwars.lobby.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.minigame.api.cosmetic.menu.MenuSlot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record LobbyMenuSettings(Mode mode, Duels duels, Maps maps, TopModes topModes, Stats stats) {

    public static LobbyMenuSettings from(ConfigurationSection menus) {
        return new LobbyMenuSettings(Mode.from(section(menus, "mode")), Duels.from(section(menus, "duels")), Maps.from(section(menus, "maps")),
            TopModes.from(section(menus, "top-modes")), Stats.from(section(menus, "stats")));
    }

    static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }

    public record Mode(String title, int rows, MenuSlot play, List<String> topLines, String topEmpty, MenuSlot map, MenuSlot training, MenuSlot back, List<String> backModes) {

        static Mode from(ConfigurationSection section) {
            return new Mode(section.getString("title", "{mode}"), section.getInt("rows", 3), MenuSlot.from(section(section, "play")),
                section.getStringList("top-lines"), section.getString("top-empty", ""), MenuSlot.from(section(section, "map")),
                MenuSlot.from(section(section, "training")), MenuSlot.from(section(section, "back")), section.getStringList("back-modes"));
        }
    }

    public record Duels(String title, int rows, ItemBuilder item, Map<String, DuelEntry> entries) {

        static Duels from(ConfigurationSection section) {
            Map<String, DuelEntry> entries = new LinkedHashMap<>();
            ConfigurationSection configured = section(section, "entries");
            for (String mode : configured.getKeys(false)) {
                ConfigurationSection entry = section(configured, mode);
                entries.put(mode, new DuelEntry(entry.getInt("slot"), entry.getInt("amount", 1)));
            }
            return new Duels(section.getString("title", ""), section.getInt("rows", 3), ItemBuilder.from(section(section, "item")), entries);
        }
    }

    public record DuelEntry(int slot, int amount) {
    }

    public record Maps(String title, int rows, List<Integer> slots, ItemBuilder favourite, ItemBuilder other, MenuSlot back, MenuSlot random, MenuSlot search, MenuSlot previous, MenuSlot next) {

        static Maps from(ConfigurationSection section) {
            return new Maps(section.getString("title", ""), section.getInt("rows", 6), section.getIntegerList("slots"), ItemBuilder.from(section(section, "favourite")),
                ItemBuilder.from(section(section, "other")), MenuSlot.from(section(section, "back")), MenuSlot.from(section(section, "random")),
                MenuSlot.from(section(section, "search")), MenuSlot.from(section(section, "previous")), MenuSlot.from(section(section, "next")));
        }
    }

    public record TopModes(String title, int rows, List<String> selectedLore, List<String> otherLore, Map<String, TopModeEntry> entries) {

        static TopModes from(ConfigurationSection section) {
            Map<String, TopModeEntry> entries = new LinkedHashMap<>();
            ConfigurationSection configured = section(section, "entries");
            for (String mode : configured.getKeys(false)) {
                ConfigurationSection entry = section(configured, mode);
                entries.put(mode, new TopModeEntry(entry.getInt("slot"), entry.getString("title", mode), ItemBuilder.from(section(entry, "item"))));
            }
            return new TopModes(section.getString("title", ""), section.getInt("rows", 4), section.getStringList("selected-lore"), section.getStringList("other-lore"), entries);
        }
    }

    public record TopModeEntry(int slot, String title, ItemBuilder item) {
    }

    public record Stats(String title, int rows, MenuSlot general, List<String> lore, Map<String, MenuSlot> entries) {

        static Stats from(ConfigurationSection section) {
            Map<String, MenuSlot> entries = new LinkedHashMap<>();
            ConfigurationSection configured = section(section, "entries");
            for (String mode : configured.getKeys(false)) {
                entries.put(mode, MenuSlot.from(section(configured, mode)));
            }
            return new Stats(section.getString("title", ""), section.getInt("rows", 6), MenuSlot.from(section(section, "general")), section.getStringList("lore"), entries);
        }
    }
}
