package br.com.muxe.lobby.config;

import br.com.muxe.core.bukkit.bossbar.BossBarFrames;
import br.com.muxe.core.common.config.source.DurationParser;
import br.com.muxe.lobby.hotbar.HotbarSettings;
import br.com.muxe.lobby.profile.PlayerInfo;
import br.com.muxe.lobby.scoreboard.ScoreboardTemplate;
import br.com.muxe.lobby.selector.SelectorSettings;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record LobbySettings(
    List<String> games,
    int voidLevel,
    double flyHeight,
    int spamMaxClicks,
    Duration spamWindow,
    Duration spamCooldown,
    BossBarFrames bossBar,
    ScoreboardTemplate scoreboard,
    Duration scoreboardInterval,
    PlayerInfo defaultInfo,
    HotbarSettings hotbar,
    SelectorSettings selector
) {

    private static final long MILLIS_PER_TICK = 50;

    public static LobbySettings from(ConfigurationSection config) {
        return new LobbySettings(
            config.getStringList("play.games"),
            config.getInt("protection.void-level", 0),
            config.getDouble("arrival.fly-height", 5),
            config.getInt("spam.max-clicks", 3),
            duration(config, "spam.window", "1s"),
            duration(config, "spam.cooldown", "3s"),
            new BossBarFrames(config.getStringList("bossbar.frames"), ticks(duration(config, "bossbar.interval", "500ms"))),
            new ScoreboardTemplate(config.getString("scoreboard.title", "&b&lMUXE"), config.getStringList("scoreboard.lines")),
            duration(config, "scoreboard.update-interval", "1s"),
            new PlayerInfo(config.getString("scoreboard.default-tag", "&7Membro"), config.getLong("scoreboard.default-credits", 0)),
            HotbarSettings.from(section(config, "hotbar")),
            SelectorSettings.from(section(config, "lobby-selector"))
        );
    }

    public static long ticks(Duration duration) {
        return Math.max(1, duration.toMillis() / MILLIS_PER_TICK);
    }

    private static Duration duration(ConfigurationSection config, String path, String fallback) {
        return DurationParser.parse(config.getString(path, fallback));
    }

    private static ConfigurationSection section(ConfigurationSection config, String path) {
        return Objects.requireNonNull(config.getConfigurationSection(path), "Missing configuration section " + path);
    }
}
