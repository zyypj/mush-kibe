package br.com.muxe.core.bukkit.selector.game;

import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record GameSelectorSettings(String title, int rows, List<ConfiguredButton> buttons) {

    public GameSelectorSettings {
        Objects.requireNonNull(title, "title");
        buttons = List.copyOf(buttons);
        ConfiguredButton.requireFit(buttons, rows);
    }

    public static GameSelectorSettings from(ConfigurationSection section) {
        return new GameSelectorSettings(
            section.getString("title", "Selecionar jogo"),
            section.getInt("rows", 4),
            ConfiguredButton.list(section.getConfigurationSection("buttons"))
        );
    }
}
