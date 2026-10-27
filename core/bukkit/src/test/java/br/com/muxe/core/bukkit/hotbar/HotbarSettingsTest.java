package br.com.muxe.core.bukkit.hotbar;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class HotbarSettingsTest {

    private final HotbarSettings settings = HotbarSettings.from(ShopFixtures.CONFIG.getConfigurationSection("hotbar"));

    @Test
    void theHotbarIsGivenOnLobbiesOnly() {
        assertThat(settings.appliesTo(ServerKind.LOBBY)).isTrue();
        assertThat(settings.appliesTo(ServerKind.AUTH)).isFalse();
        assertThat(settings.appliesTo(ServerKind.MINIGAME)).isFalse();
    }

    @Test
    void itemsSitInTheReferenceSlots() {
        assertThat(settings.actionAt(0)).contains(HotbarAction.GAME_SELECTOR);
        assertThat(settings.actionAt(1)).contains(HotbarAction.PROFILE);
        assertThat(settings.actionAt(4)).contains(HotbarAction.COSMETICS);
        assertThat(settings.actionAt(7)).contains(HotbarAction.VISIBILITY);
        assertThat(settings.actionAt(8)).contains(HotbarAction.LOBBY_SELECTOR);
        assertThat(settings.actionAt(2)).isEmpty();
        assertThat(settings.gameSelector().item().material()).isEqualTo(Material.COMPASS);
        assertThat(settings.gameSelector().item().name()).isEqualTo("§aSelecionar jogo");
        assertThat(settings.profile().item().name()).isEqualTo("§aMeu perfil");
        assertThat(settings.cosmetics().item().name()).isEqualTo("§aCosméticos");
        assertThat(settings.lobbySelector().item().name()).isEqualTo("§aSelecionar lobby");
        assertThat(settings.lobbySelector().item().isGlowing()).isTrue();
    }

    @Test
    void theVisibilitySelectorUsesTheThreeDyes() {
        assertThat(settings.visibilityItem(Visibility.ALL).data()).isEqualTo((short) 10);
        assertThat(settings.visibilityItem(Visibility.ALL).name()).isEqualTo("§fJogadores: §aAtivados");
        assertThat(settings.visibilityItem(Visibility.FRIENDS).data()).isEqualTo((short) 12);
        assertThat(settings.visibilityItem(Visibility.FRIENDS).name()).isEqualTo("§fJogadores: §bApenas amigos");
        assertThat(settings.visibilityItem(Visibility.NONE).data()).isEqualTo((short) 8);
        assertThat(settings.visibilityItem(Visibility.NONE).name()).isEqualTo("§fJogadores: §7Desativados");
    }

    @Test
    void eachItemRunsItsOwnAction() {
        List<String> ran = new ArrayList<>();
        HotbarActions actions = new HotbarActions(
            player -> ran.add("games"),
            player -> ran.add("profile"),
            player -> ran.add("cosmetics"),
            player -> ran.add("visibility"),
            player -> ran.add("lobbies")
        );
        Player player = ShopFixtures.player("Steve");

        for (HotbarAction action : HotbarAction.values()) {
            actions.run(action, player);
        }

        assertThat(ran).containsExactly("games", "profile", "cosmetics", "visibility", "lobbies");
    }

    @Test
    void visibilityAndLobbyMessagesMatchTheReferenceScreenshots() {
        MessageCatalog messages = MessageCatalog.from(ShopFixtures.CONFIG.getConfigurationSection("messages"));

        assertThat(messages.get("visibility-all")).isEqualTo("§aVocê agora está vendo todos os jogadores.");
        assertThat(messages.get("visibility-friends")).isEqualTo("§bVocê agora está vendo apenas seus amigos.");
        assertThat(messages.get("visibility-none")).isEqualTo("§cVocê escondeu todos os jogadores.");
        assertThat(messages.get("lobby-current")).isEqualTo("§cVocê já está conectado neste lobby.");
        assertThat(messages.get("spam-cooldown", "seconds", "2,9")).isEqualTo("§cAguarde 2,9s para usar novamente.");
    }
}
