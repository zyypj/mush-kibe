package br.com.muxe.lobby.config;

import br.com.muxe.core.bukkit.config.edit.ConfigReloads;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.config.live.LiveValue;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.lobby.scoreboard.ScoreboardTemplate;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;

public final class LobbyReloads {

    public static final String SPAWN = "spawn";
    public static final String SCOREBOARD_TITLE = "scoreboard.title";
    public static final String SCOREBOARD_LINES = "scoreboard.lines";

    private LobbyReloads() {
    }

    public static ConfigReloads create(LiveValue<Localized<ScoreboardTemplate>> scoreboard, Function<ConfigurationSection, LocalizedConfig> texts, Runnable spawn) {
        return new ConfigReloads()
            .register(SPAWN, SPAWN, config -> spawn.run())
            .register(SCOREBOARD_TITLE, SCOREBOARD_TITLE, config -> scoreboard.reload(() -> texts.apply(config).map(ScoreboardTemplate::from)))
            .register(SCOREBOARD_LINES, SCOREBOARD_LINES, config -> scoreboard.reload(() -> texts.apply(config).map(ScoreboardTemplate::from)));
    }
}
