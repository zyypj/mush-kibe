package br.com.muxe.minigame.engine.mode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GameModeLoaderTest {

    @Test
    void loadsEveryConfiguredMode() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
            modes:
              solo:
                display-name: Solo
                min-players: 2
                max-players: 12
                countdown: 20s
              duo:
                display-name: Duo
                min-players: 4
                max-players: 24
            """);

        List<GameMode> modes = GameModeLoader.load(config.getConfigurationSection("modes"));

        assertThat(modes).containsExactly(
            new GameMode("solo", "Solo", 2, 12, Duration.ofSeconds(20)),
            new GameMode("duo", "Duo", 4, 24, Duration.ofSeconds(15))
        );
    }

    @Test
    void returnsNothingWithoutModes() {
        assertThat(GameModeLoader.load(null)).isEmpty();
    }
}
