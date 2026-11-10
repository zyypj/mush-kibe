package br.com.muxe.core.bukkit.selector.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.menu.button.MenuFixtures;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.testing.TestNetwork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LocalizedGameSelectorTest {

    private final Localized<GameSelectorSettings> settings = LocalizedConfig.bundled(GameSelectorSettings.class, "config.yml")
        .map(root -> GameSelectorSettings.from(root.getConfigurationSection("menus.game-selector")));
    private final TestNetwork network = new TestNetwork();
    private final ServerDirectory servers = MenuFixtures.servers();

    @AfterEach
    void close() {
        network.close();
    }

    @Test
    void eachLanguageGetsItsOwnTitleAndLoreWithTheSameSlots() {
        MenuFixtures.online(servers, "bedwars-1", "bedwars", 12);
        GameSelectorMenu selector = new GameSelectorMenu(settings, MenuFixtures.actions(network, servers), player -> Language.ENGLISH);

        Menu english = selector.menu(Language.ENGLISH);
        Menu portuguese = selector.menu(Language.PORTUGUESE);

        assertThat(english.title()).isEqualTo("Select game");
        assertThat(portuguese.title()).isEqualTo("Selecionar jogo");
        assertThat(english.buttons().keySet()).isEqualTo(portuguese.buttons().keySet());
        assertThat(icon(english, 10).lore()).containsExactly("§e12 playing now!", " ", "§aClick to join!");
        assertThat(icon(portuguese, 10).lore()).containsExactly("§e12 jogando agora!", " ", "§aClique para entrar!");
        assertThat(icon(english, 10).material()).isEqualTo(icon(portuguese, 10).material());
    }

    private static ItemBuilder icon(Menu menu, int slot) {
        return menu.button(slot).map(MenuButton::icon).orElseThrow();
    }
}
