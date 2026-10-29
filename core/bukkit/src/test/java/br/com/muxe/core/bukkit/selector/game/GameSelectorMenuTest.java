package br.com.muxe.core.bukkit.selector.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.menu.button.ButtonAction;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.bukkit.menu.button.MenuFixtures;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.testing.TestNetwork;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GameSelectorMenuTest {

    private final GameSelectorSettings settings = GameSelectorSettings.from(ShopFixtures.CONFIG.getConfigurationSection("menus.game-selector"));
    private final TestNetwork network = new TestNetwork();
    private final ServerDirectory servers = MenuFixtures.servers();

    @AfterEach
    void close() {
        network.close();
    }

    @Test
    void theMenuHasFourRowsWithTheGamesInTheReferenceSlots() {
        assertThat(settings.title()).isEqualTo("Selecionar jogo");
        assertThat(settings.rows()).isEqualTo(4);
        assertThat(settings.buttons()).extracting(ConfiguredButton::slot)
            .containsExactly(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 35);
        assertThat(settings.buttons().getFirst().action()).isEqualTo(ButtonAction.game("bedwars"));
        assertThat(settings.buttons().getLast().action()).isEqualTo(ButtonAction.shop("ultra-plus"));
    }

    @Test
    void eachGameShowsHowManyPlayAcrossItsServers() {
        MenuFixtures.online(servers, "bedwars-1", "bedwars", 200);
        MenuFixtures.online(servers, "bedwars-2", "bedwars", 64);

        Menu menu = new GameSelectorMenu(settings, MenuFixtures.actions(network, servers)).menu();

        ItemBuilder bedwars = icon(menu, 10);
        assertThat(bedwars.material()).isEqualTo(Material.BED);
        assertThat(bedwars.name()).isEqualTo("§b§lBED WARS");
        assertThat(bedwars.lore()).containsExactly("§e264 jogando agora!", " ", "§aClique para entrar!");
        assertThat(icon(menu, 11).lore().getFirst()).isEqualTo("§e0 jogando agora!");
    }

    @Test
    void theBridgeTrainingGlowsAndTheCustomRoomPointsToTheShop() {
        Menu menu = new GameSelectorMenu(settings, MenuFixtures.actions(network, servers)).menu();

        assertThat(icon(menu, 24).isGlowing()).isTrue();
        assertThat(icon(menu, 24).lore()).containsExactly(" ", "§aClique para entrar!");
        assertThat(icon(menu, 25).name()).isEqualTo("§b§lNASCÓ MANDOU");
        assertThat(icon(menu, 35).name()).isEqualTo("§aSala customizada (/salacustom)");
        assertThat(icon(menu, 35).lore()).contains("§7Exclusivo para §d§lULTRA§6+", "§eClique para comprar.");
        assertThat(menu.button(0)).isEmpty();
    }

    private static ItemBuilder icon(Menu menu, int slot) {
        return menu.button(slot).map(MenuButton::icon).orElseThrow();
    }
}
