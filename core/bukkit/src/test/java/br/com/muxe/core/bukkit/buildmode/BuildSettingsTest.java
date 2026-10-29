package br.com.muxe.core.bukkit.buildmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.server.ServerKind;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class BuildSettingsTest {

    private static final YamlConfiguration CONFIG = defaultConfig();

    private final BuildSettings settings = BuildSettings.from(CONFIG.getConfigurationSection("build-mode"));

    @Test
    void lobbiesAndTheAuthServerKeepPlayersInAdventure() {
        assertThat(settings.adventureOnly(ServerKind.LOBBY)).isTrue();
        assertThat(settings.adventureOnly(ServerKind.AUTH)).isTrue();
        assertThat(settings.adventureOnly(ServerKind.MINIGAME)).isFalse();
    }

    @Test
    void leavingBuildModeReturnsToAdventureWhereItIsRequired() {
        assertThat(settings.restingMode(ServerKind.LOBBY, GameMode.SURVIVAL)).isEqualTo(GameMode.ADVENTURE);
        assertThat(settings.restingMode(ServerKind.MINIGAME, GameMode.SURVIVAL)).isEqualTo(GameMode.SURVIVAL);
    }

    @Test
    void buildModeIsRememberedForSixHours() {
        assertThat(settings.duration()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void messagesComeFromTheCoreConfiguration() {
        MessageCatalog messages = MessageCatalog.from(CONFIG.getConfigurationSection("messages"));

        assertThat(messages.get("build-action-bar")).isEqualTo("§aModo construção ativado");
        assertThat(messages.get("build-enabled")).isEqualTo("§aModo construção ativado.");
        assertThat(messages.get("build-disabled")).isEqualTo("§cModo construção desativado.");
        assertThat(messages.get("build-usage")).isEqualTo("§eUso: /build <on|off>");
    }

    @Test
    void unknownServerKindsAreRejected() throws InvalidConfigurationException {
        YamlConfiguration broken = new YamlConfiguration();
        broken.loadFromString("build-mode:\n  adventure-only:\n    - CASTLE\n");

        assertThatThrownBy(() -> BuildSettings.from(broken.getConfigurationSection("build-mode"))).isInstanceOf(IllegalArgumentException.class);
    }

    private static YamlConfiguration defaultConfig() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(BuildSettingsTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
