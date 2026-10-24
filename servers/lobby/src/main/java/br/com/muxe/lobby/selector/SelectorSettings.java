package br.com.muxe.lobby.selector;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record SelectorSettings(String group, String titleTemplate, ItemBuilder current, ItemBuilder other, ItemBuilder previousPage, ItemBuilder nextPage) {

    public static SelectorSettings from(ConfigurationSection section) {
        return new SelectorSettings(
            section.getString("group", "lobby"),
            section.getString("title", "Selecionar lobby ({page}/{pages})"),
            ItemBuilder.from(child(section, "current")),
            ItemBuilder.from(child(section, "other")),
            ItemBuilder.from(child(section, "previous-page")),
            ItemBuilder.from(child(section, "next-page"))
        );
    }

    public String title(int page, int pages) {
        return titleTemplate.replace("{page}", Integer.toString(page)).replace("{pages}", Integer.toString(pages));
    }

    private static ConfigurationSection child(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
