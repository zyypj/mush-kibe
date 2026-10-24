package br.com.muxe.lobby.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.hotbar.HotbarAction;
import br.com.muxe.lobby.visibility.domain.Visibility;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LobbySettingsTest {

    private static final YamlConfiguration CONFIG = defaultConfig();

    private final LobbySettings settings = LobbySettings.from(CONFIG);

    @Test
    void hotbarItemsSitInTheReferenceSlots() {
        assertThat(settings.hotbar().actionAt(0)).contains(HotbarAction.GAME_SELECTOR);
        assertThat(settings.hotbar().actionAt(1)).contains(HotbarAction.PROFILE);
        assertThat(settings.hotbar().actionAt(4)).contains(HotbarAction.COSMETICS);
        assertThat(settings.hotbar().actionAt(7)).contains(HotbarAction.VISIBILITY);
        assertThat(settings.hotbar().actionAt(8)).contains(HotbarAction.LOBBY_SELECTOR);
        assertThat(settings.hotbar().actionAt(2)).isEmpty();
        assertThat(settings.hotbar().lobbySelector().item().material()).isEqualTo(Material.NETHER_STAR);
        assertThat(settings.hotbar().lobbySelector().item().name()).isEqualTo("§aSelecionar lobby");
        assertThat(settings.hotbar().gameSelector().item().name()).isEqualTo("§aSelecionar jogo");
        assertThat(settings.hotbar().profile().item().name()).isEqualTo("§aMeu perfil");
        assertThat(settings.hotbar().cosmetics().item().name()).isEqualTo("§aCosméticos");
    }

    @Test
    void theVisibilitySelectorUsesTheThreeDyes() {
        assertThat(settings.hotbar().visibilityItem(Visibility.ALL).data()).isEqualTo((short) 10);
        assertThat(settings.hotbar().visibilityItem(Visibility.ALL).name()).isEqualTo("§fJogadores: §aAtivados");
        assertThat(settings.hotbar().visibilityItem(Visibility.FRIENDS).data()).isEqualTo((short) 6);
        assertThat(settings.hotbar().visibilityItem(Visibility.FRIENDS).name()).isEqualTo("§fJogadores: §bApenas amigos");
        assertThat(settings.hotbar().visibilityItem(Visibility.NONE).data()).isEqualTo((short) 8);
        assertThat(settings.hotbar().visibilityItem(Visibility.NONE).name()).isEqualTo("§fJogadores: §7Desativados");
    }

    @Test
    void theLobbySelectorShowsGreenForTheCurrentLobbyAndGrayForTheOthers() {
        assertThat(settings.selector().current().data()).isEqualTo((short) 5);
        assertThat(settings.selector().other().data()).isEqualTo((short) 7);
        assertThat(settings.selector().current().replacing("number", "3", "online", "32", "max", "60").name()).isEqualTo("§aLobby Principal #3");
        assertThat(settings.selector().other().replacing("number", "3", "online", "32", "max", "60").lore()).containsExactly("§fJogadores §a32/60");
        assertThat(settings.selector().title(1, 1)).isEqualTo("Selecionar lobby (1/1)");
    }

    @Test
    void theBossBarAlternatesYellowAndGoldEveryHalfSecond() {
        assertThat(settings.bossBar().ticksPerFrame()).isEqualTo(10);
        assertThat(settings.bossBar().at(0)).isEqualTo("§e§lMUXE");
        assertThat(settings.bossBar().at(10)).isEqualTo("§6§lMUXE");
    }

    @Test
    void spamAndArrivalFollowTheSpecification() {
        assertThat(settings.spamMaxClicks()).isEqualTo(3);
        assertThat(settings.spamCooldown()).isEqualTo(Duration.ofSeconds(3));
        assertThat(settings.flyHeight()).isEqualTo(5);
        assertThat(settings.scoreboard().title()).isEqualTo("&b&lMUXE");
        assertThat(settings.defaultInfo().tag()).isEqualTo("&7Membro");
    }

    @Test
    void messagesMatchTheReferenceScreenshots() {
        MessageCatalog messages = MessageCatalog.from(CONFIG.getConfigurationSection("messages"));

        assertThat(messages.get("visibility-all")).isEqualTo("§aVocê agora está vendo todos os jogadores.");
        assertThat(messages.get("visibility-friends")).isEqualTo("§bVocê agora está vendo apenas seus amigos.");
        assertThat(messages.get("visibility-none")).isEqualTo("§cVocê escondeu todos os jogadores.");
        assertThat(messages.get("spam-cooldown", "seconds", "2,9")).isEqualTo("§cAguarde 2,9s para usar novamente.");
        assertThat(messages.get("build-action-bar")).isEqualTo("§aModo construção ativado");
        assertThat(messages.get("build-enabled")).isEqualTo("§aModo construção ativado.");
        assertThat(messages.get("build-disabled")).isEqualTo("§cModo construção desativado.");
    }

    private static YamlConfiguration defaultConfig() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(LobbySettingsTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
