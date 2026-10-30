package br.com.muxe.minigame.engine.mode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
              duo:
                display-name: Duo
                min-players: 4
                max-players: 24
                team-size: 2
            """);

        List<GameMode> modes = GameModeLoader.load(config.getConfigurationSection("modes"));

        assertThat(modes).containsExactly(
            new GameMode("solo", "Solo", 2, 12, 1),
            new GameMode("duo", "Duo", 4, 24, 2)
        );
    }

    @Test
    void aModeKnowsItsTeamsAndWhetherItStartsFull() {
        GameMode quartets = new GameMode("quartetos", "Quartetos", 8, 16, 4);
        GameMode fours = new GameMode("4s", "4s", 8, 8, 4);

        assertThat(quartets.teams()).isEqualTo(4);
        assertThat(quartets.startsFull()).isFalse();
        assertThat(fours.teams()).isEqualTo(2);
        assertThat(fours.startsFull()).isTrue();
        assertThatThrownBy(() -> new GameMode("trios", "Trios", 6, 13, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsNothingWithoutModes() {
        assertThat(GameModeLoader.load(null)).isEmpty();
    }
}
