package br.com.muxe.minigame.bedwars.lobby.settings;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public record LobbyScoreboardSettings(String title, List<String> lines, List<String> modeLines, List<String> defaultModes, int shownModes, int barSegments) {

    public static final String MODES = "{modes}";

    public LobbyScoreboardSettings {
        lines = List.copyOf(lines);
        modeLines = List.copyOf(modeLines);
        defaultModes = List.copyOf(defaultModes);
    }

    public static LobbyScoreboardSettings from(ConfigurationSection section) {
        return new LobbyScoreboardSettings(
            section.getString("title", ""),
            section.getStringList("lines"),
            section.getStringList("mode-lines"),
            section.getStringList("default-modes"),
            section.getInt("shown-modes", 2),
            section.getInt("bar-segments", 10)
        );
    }
}
