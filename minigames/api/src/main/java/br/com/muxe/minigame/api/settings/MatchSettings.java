package br.com.muxe.minigame.api.settings;

import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.minigame.engine.match.CountdownTitles;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.ModeNames;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record MatchSettings(
    String lobbyGroup,
    String defaultMode,
    Timings timings,
    CountdownTitles countdownTitles,
    BoardSettings board,
    WaitingHotbar waitingHotbar,
    SpectatorHotbar spectatorHotbar,
    Teleporter teleporter,
    MatchSounds sounds,
    KnockbackChoice knockback,
    Map<String, String> modeNames
) {

    public MatchSettings {
        modeNames = Map.copyOf(modeNames);
    }

    public static MatchSettings from(ConfigurationSection config) {
        return new MatchSettings(
            config.getString("lobby-group", "lobby"),
            config.getString("default-mode", "solo"),
            Timings.from(config.getConfigurationSection("timings")),
            CountdownTitles.from(config.getConfigurationSection("countdown.titles")),
            BoardSettings.from(config.getConfigurationSection("scoreboard")),
            WaitingHotbar.from(config.getConfigurationSection("hotbar.waiting")),
            SpectatorHotbar.from(config.getConfigurationSection("hotbar.spectator")),
            Teleporter.from(config.getConfigurationSection("hotbar.teleporter")),
            MatchSounds.from(config.getConfigurationSection("sounds")),
            KnockbackChoice.from(DefaultedSections.of(config, "knockback")),
            ModeNames.read(config.getConfigurationSection(ModeNames.MODES))
        );
    }

    public String modeName(GameMode mode) {
        return modeNames.getOrDefault(mode.id(), mode.displayName());
    }
}
