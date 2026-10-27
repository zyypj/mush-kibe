package br.com.muxe.lobby.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LobbySettingsTest {

    private static final YamlConfiguration CONFIG = defaultConfig();

    private final LobbySettings settings = LobbySettings.from(CONFIG);

    @Test
    void theBossBarAlternatesYellowAndGoldEveryHalfSecond() {
        assertThat(settings.bossBar().ticksPerFrame()).isEqualTo(10);
        assertThat(settings.bossBar().at(0)).isEqualTo("§e§lMUXE");
        assertThat(settings.bossBar().at(10)).isEqualTo("§6§lMUXE");
    }

    @Test
    void arrivalAndScoreboardFollowTheSpecification() {
        assertThat(settings.flyHeight()).isEqualTo(5);
        assertThat(settings.scoreboard().title()).isEqualTo("&b&lMUXE");
        assertThat(settings.defaultInfo().tag()).isEqualTo("&7Membro");
    }

    @Test
    void messagesMatchTheReferenceScreenshots() {
        MessageCatalog messages = MessageCatalog.from(CONFIG.getConfigurationSection("messages"));

        assertThat(messages.get("play-connecting", "game", "skywars")).isEqualTo("§7Procurando uma sala de §bskywars§7...");
        assertThat(messages.get("spawn-set")).isEqualTo("§aSpawn do lobby definido.");
    }

    private static YamlConfiguration defaultConfig() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(LobbySettingsTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
