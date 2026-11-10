package br.com.muxe.minigame.engine.mode;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ModeNamesTest {

    @Test
    void aModeIsNamedInTheReadersLanguageAndFallsBackToPortuguese() throws InvalidConfigurationException {
        YamlConfiguration standard = new YamlConfiguration();
        standard.loadFromString("""
            modes:
              duplas:
                display-name: Duplas
                max-players: 16
                team-size: 2
              solo:
                display-name: Solo
            """);
        YamlConfiguration english = new YamlConfiguration();
        english.loadFromString("""
            modes:
              duplas:
                display-name: Doubles
            """);
        LocalizedConfig texts = LocalizedConfig.of(standard, Map.of(Language.ENGLISH, english));

        ModeNames names = GameModeLoader.names(texts);

        assertThat(names.in(Language.PORTUGUESE, "duplas")).isEqualTo("Duplas");
        assertThat(names.in(Language.ENGLISH, "duplas")).isEqualTo("Doubles");
        assertThat(names.in(Language.ENGLISH, "solo")).isEqualTo("Solo");
        assertThat(names.in(Language.ENGLISH, "unknown")).isEqualTo("unknown");
        assertThat(GameModeLoader.modes(texts)).extracting(GameMode::displayName).containsExactly("Duplas", "Solo");
    }

    @Test
    void modesWithoutTranslationsKeepTheirDisplayName() {
        ModeNames names = ModeNames.of(List.of(new GameMode("solo", "Solo", 2, 8, 1)));

        assertThat(names.in(Language.ENGLISH, "solo")).isEqualTo("Solo");
    }
}
