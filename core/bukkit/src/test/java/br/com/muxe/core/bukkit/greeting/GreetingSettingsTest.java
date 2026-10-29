package br.com.muxe.core.bukkit.greeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.server.ServerKind;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.Sound;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GreetingSettingsTest {

    private static final YamlConfiguration CONFIG = defaultConfig();

    private final GreetingSettings settings = GreetingSettings.from(CONFIG.getConfigurationSection("join-greeting"));

    @Test
    void onlyLobbiesGreetPlayers() {
        assertThat(settings.appliesTo(ServerKind.LOBBY)).isTrue();
        assertThat(settings.appliesTo(ServerKind.AUTH)).isFalse();
        assertThat(settings.appliesTo(ServerKind.MINIGAME)).isFalse();
    }

    @Test
    void theGreetingPlaysASoundAfterAShortDelay() {
        assertThat(settings.sound()).isEqualTo(Sound.LEVEL_UP);
        assertThat(settings.delayTicks()).isEqualTo(10);
        assertThat(settings.volume()).isEqualTo(1f);
    }

    @Test
    void theTitleMatchesTheReferenceScreenshot() {
        MessageCatalog messages = MessageCatalog.from(CONFIG.getConfigurationSection("messages"));

        assertThat(messages.get("join-title")).isEqualTo("§b§lMUXE");
        assertThat(messages.get("join-subtitle")).isEqualTo("§eBoas-vindas!");
        assertThat(settings.stayTicks()).isPositive();
    }

    @Test
    void theConnectingMessageIsGone() {
        assertThat(CONFIG.isSet("messages.connecting")).isFalse();
    }

    @Test
    void aMissingSectionTurnsTheGreetingOff() {
        GreetingSettings off = GreetingSettings.from(null);

        assertThat(off.appliesTo(ServerKind.LOBBY)).isFalse();
    }

    @Test
    void unknownSoundsAndKindsAreRejected() throws InvalidConfigurationException {
        YamlConfiguration broken = new YamlConfiguration();
        broken.loadFromString("join-greeting:\n  sound: NOT_A_SOUND\n");
        YamlConfiguration wrongKind = new YamlConfiguration();
        wrongKind.loadFromString("join-greeting:\n  server-kinds:\n    - CASTLE\n");

        assertThatThrownBy(() -> GreetingSettings.from(broken.getConfigurationSection("join-greeting"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GreetingSettings.from(wrongKind.getConfigurationSection("join-greeting"))).isInstanceOf(IllegalArgumentException.class);
    }

    private static YamlConfiguration defaultConfig() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(GreetingSettingsTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
