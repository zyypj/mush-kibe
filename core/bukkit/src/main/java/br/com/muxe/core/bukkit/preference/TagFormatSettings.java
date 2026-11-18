package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.common.preference.domain.TagFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record TagFormatSettings(String title, int rows, String example, ConfiguredButton back, Texts texts, List<Option> options) {

    public TagFormatSettings {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(back, "back");
        Objects.requireNonNull(texts, "texts");
        options = List.copyOf(options);
    }

    public static TagFormatSettings from(ConfigurationSection section) {
        List<Option> options = new ArrayList<>();
        ConfigurationSection formats = section(section, "formats");
        for (String key : formats.getKeys(false)) {
            ConfigurationSection option = section(formats, key);
            TagFormat format = TagFormat.parse(key.toUpperCase(Locale.ROOT).replace('-', '_'))
                .orElseThrow(() -> new IllegalArgumentException("Unknown tag format '" + key + "' at " + formats.getCurrentPath()));
            options.add(new Option(format, option.getInt("slot"), option.getBoolean("exclusive", true), ItemBuilder.from(section(option, "item"))));
        }
        ConfigurationSection texts = section(section, "texts");
        return new TagFormatSettings(section.getString("title", "Formato da tag"), section.getInt("rows", 3), section.getString("example", "{prefix}{color}{player}"),
            ConfiguredButton.from(section(section, "back")),
            new Texts(texts.getString("example", "&7Exemplo:"), texts.getString("exclusive", "&7Exclusivo para &d&lULTRA&6+"), texts.getString("selected", "&aSelecionado."),
                texts.getString("select", "&eClique para selecionar."), texts.getString("locked", "&cBloqueado.")),
            options);
    }

    public record Texts(String example, String exclusive, String selected, String select, String locked) {
    }

    public record Option(TagFormat format, int slot, boolean exclusive, ItemBuilder item) {

        public Option {
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(item, "item");
        }
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
