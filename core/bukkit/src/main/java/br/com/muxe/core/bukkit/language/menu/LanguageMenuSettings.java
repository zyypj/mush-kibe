package br.com.muxe.core.bukkit.language.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.common.language.domain.Language;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record LanguageMenuSettings(String title, int rows, Map<Language, Choice> choices) {

    public record Choice(int slot, ItemBuilder item) {

        public Choice {
            Objects.requireNonNull(item, "item");
        }
    }

    public LanguageMenuSettings {
        Objects.requireNonNull(title, "title");
        choices = Collections.unmodifiableMap(new EnumMap<>(choices));
    }

    public static LanguageMenuSettings from(ConfigurationSection section) {
        Objects.requireNonNull(section, "language-menu");
        int rows = section.getInt("rows", 3);
        Map<Language, Choice> choices = new EnumMap<>(Language.class);
        ConfigurationSection languages = Objects.requireNonNull(section.getConfigurationSection("languages"), "language-menu.languages");
        for (String tag : languages.getKeys(false)) {
            Language language = Language.fromTag(tag).orElseThrow(() -> new IllegalArgumentException("Unknown language '" + tag + "' in language-menu.languages"));
            ConfigurationSection entry = languages.getConfigurationSection(tag);
            int slot = entry.getInt("slot", -1);
            if (slot < 0 || slot >= rows * 9) {
                throw new IllegalArgumentException("language-menu.languages." + tag + ".slot is outside the menu");
            }
            choices.put(language, new Choice(slot, ItemBuilder.from(Objects.requireNonNull(entry.getConfigurationSection("item"), "language-menu.languages." + tag + ".item"))));
        }
        return new LanguageMenuSettings(section.getString("title", "Selecionar idioma"), rows, choices);
    }
}
