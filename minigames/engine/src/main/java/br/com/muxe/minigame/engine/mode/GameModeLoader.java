package br.com.muxe.minigame.engine.mode;

import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.common.config.source.ConfigSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public final class GameModeLoader {

    private GameModeLoader() {
    }

    public static List<GameMode> load(ConfigurationSection modes) {
        List<GameMode> loaded = new ArrayList<>();
        if (modes == null) {
            return loaded;
        }
        for (String id : modes.getKeys(false)) {
            ConfigSource mode = new BukkitConfigSource(modes.getConfigurationSection(id));
            loaded.add(new GameMode(
                id,
                mode.string("display-name", id),
                mode.integer("min-players", 2),
                mode.integer("max-players", 8),
                mode.duration("countdown", Duration.ofSeconds(15))
            ));
        }
        return List.copyOf(loaded);
    }
}
