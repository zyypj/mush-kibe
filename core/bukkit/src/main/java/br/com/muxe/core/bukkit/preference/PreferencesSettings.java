package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.preference.domain.PrivateMessages;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record PreferencesSettings(String title, int rows, int firstPane, ConfiguredButton back, List<String> tabSelected, List<String> tabUnselected,
                                  Texts texts, List<Tab> tabs) {

    public PreferencesSettings {
        tabSelected = List.copyOf(tabSelected);
        tabUnselected = List.copyOf(tabUnselected);
        tabs = List.copyOf(tabs);
    }

    public static PreferencesSettings from(ConfigurationSection section) {
        List<Tab> tabs = new ArrayList<>();
        ConfigurationSection tabSection = section(section, "tabs");
        for (String id : tabSection.getKeys(false)) {
            ConfigurationSection tab = section(tabSection, id);
            List<Entry> entries = new ArrayList<>();
            ConfigurationSection entrySection = section(tab, "entries");
            for (String key : entrySection.getKeys(false)) {
                ConfigurationSection entry = section(entrySection, key);
                Optional<Preference> preference = Optional.ofNullable(entry.getString("preference")).map(name -> Preference.byKey(name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown preference '" + name + "' at " + entry.getCurrentPath())));
                ConfigurationSection below = entry.getConfigurationSection("below");
                entries.add(new Entry(key, entry.getInt("slot"), ItemBuilder.from(section(entry, "item")), preference,
                    below == null ? Optional.empty() : Optional.of(Material.valueOf(below.getString("material", "STONE_BUTTON"))),
                    Optional.ofNullable(entry.getString("opens"))));
            }
            tabs.add(new Tab(id, ConfiguredButton.from(tab), entries));
        }
        return new PreferencesSettings(section.getString("title", ""), section.getInt("rows", 6), section.getInt("first-pane", 9),
            ConfiguredButton.from(section(section, "back")), section.getStringList("tab-selected"), section.getStringList("tab-unselected"),
            Texts.from(section), tabs);
    }

    public Tab tab(String id) {
        return tabs.stream().filter(tab -> tab.id().equals(id)).findFirst().orElse(tabs.getFirst());
    }

    public record Tab(String id, ConfiguredButton button, List<Entry> entries) {

        public Tab {
            entries = List.copyOf(entries);
        }
    }

    public record Entry(String id, int slot, ItemBuilder item, Optional<Preference> preference, Optional<Material> below, Optional<String> opens) {
    }

    public record Texts(String nameOn, String nameOff, String value, String enabled, String disabled, String clickEnable, String clickDisable, String optionSelected,
                        String optionOther, String clickCycle, Map<PrivateMessages, String> options, Map<PrivateMessages, Integer> optionDyes) {

        public Texts {
            options = Map.copyOf(options);
            optionDyes = Map.copyOf(optionDyes);
        }

        static Texts from(ConfigurationSection section) {
            Map<PrivateMessages, String> options = new EnumMap<>(PrivateMessages.class);
            Map<PrivateMessages, Integer> dyes = new EnumMap<>(PrivateMessages.class);
            ConfigurationSection optionSection = section(section, "options");
            for (PrivateMessages option : PrivateMessages.values()) {
                ConfigurationSection entry = section(optionSection, option.name().toLowerCase(Locale.ROOT));
                options.put(option, entry.getString("name", option.name()));
                dyes.put(option, entry.getInt("dye", 8));
            }
            return new Texts(section.getString("name-on", "&a{name}"), section.getString("name-off", "&7{name}"), section.getString("value", "{value}"),
                section.getString("enabled", ""), section.getString("disabled", ""), section.getString("click-enable", ""), section.getString("click-disable", ""),
                section.getString("option-selected", "{option}"), section.getString("option-other", "{option}"), section.getString("click-cycle", "{option}"),
                options, dyes);
        }
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
