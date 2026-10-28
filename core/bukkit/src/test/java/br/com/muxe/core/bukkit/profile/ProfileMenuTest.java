package br.com.muxe.core.bukkit.profile;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.menu.button.ButtonAction;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.bukkit.menu.button.MenuFixtures;
import br.com.muxe.core.bukkit.permission.command.RankTestKit;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProfileMenuTest {

    private final ProfileSettings settings = ProfileSettings.from(ShopFixtures.CONFIG.getConfigurationSection("menus.profile"));
    private final TestNetwork network = new TestNetwork();
    private final RankTestKit ranks = new RankTestKit();
    private final ProfileMenu profile = new ProfileMenu(settings, MenuFixtures.actions(network, MenuFixtures.servers()),
        new SimpleQueryBus(Runnable::run, List.of()), ranks, Runnable::run, Logger.getLogger("test"));

    @AfterEach
    void close() {
        network.close();
        ranks.close();
    }

    @Test
    void theHeadShowsThePlayerTheirFirstLoginAndCredits() {
        Menu menu = profile.menu(head(Optional.of(Instant.parse("2026-08-13T15:20:00Z"))));

        ItemBuilder head = icon(menu, 13);
        assertThat(menu.title()).isEqualTo("Meu perfil");
        assertThat(menu.rows()).isEqualTo(6);
        assertThat(head.material()).isEqualTo(Material.SKULL_ITEM);
        assertThat(head.skullOwner()).isEqualTo("tadeu");
        assertThat(head.name()).isEqualTo("§7tadeu");
        assertThat(head.lore()).containsExactly(" ", "§7Primeiro login: 13/08/2026 12:20", "§7Créditos: §d0");
    }

    @Test
    void theHeadShowsTheSelectedTagAndEveryRankWithItsTimeLeft() {
        ranks.online("tadeu",
            PermissionNode.group("vip", Optional.empty(), RankTestKit.NOW),
            PermissionNode.group("ultra", Optional.of(RankTestKit.NOW.plus(Duration.ofDays(30)).minusSeconds(60)), RankTestKit.NOW),
            PermissionNode.permanent("muxe.tag.natal", true, RankTestKit.NOW));
        ranks.sync.selectTag(RankTestKit.uuid("tadeu"), Optional.of("natal")).join();

        ItemBuilder head = icon(profile.menu(head(Optional.of(Instant.parse("2021-09-10T19:29:00Z")))), 13);

        assertThat(head.name()).isEqualTo("§c§lNATAL §ctadeu");
        assertThat(head.lore()).containsExactly(
            " ",
            "§7Primeiro login: 10/09/2021 16:29",
            "§7Créditos: §d0",
            "§7Rank: §aVIP §7(Vitalício)",
            "§7Rank: §dUltra §7(29d 23h)"
        );
    }

    @Test
    void anUnknownFirstLoginShowsADash() {
        assertThat(icon(profile.menu(head(Optional.empty())), 13).lore()).contains("§7Primeiro login: -");
    }

    @Test
    void theButtonsSitInTheReferenceSlotsAndOnlyTheNickOneOpensTheShop() {
        assertThat(settings.buttons()).extracting(ConfiguredButton::slot).containsExactly(29, 30, 31, 32, 33, 39, 41);
        assertThat(settings.buttons()).extracting(ConfiguredButton::action).containsExactly(
            ButtonAction.NONE, ButtonAction.NONE, ButtonAction.NONE, ButtonAction.NONE, ButtonAction.NONE, ButtonAction.shop("ultra-plus"), ButtonAction.NONE);

        Menu menu = profile.menu(head(Optional.empty()));
        assertThat(icon(menu, 29).name()).isEqualTo("§aAmigos");
        assertThat(icon(menu, 31).material()).isEqualTo(Material.DIODE);
        assertThat(icon(menu, 41).name()).isEqualTo("§aMenu do §dUltra§6+");
        assertThat(icon(menu, 41).data()).isEqualTo((short) 13);
    }

    private ProfileHead head(Optional<Instant> firstSeenAt) {
        return ProfileHead.of(ranks, RankTestKit.uuid("tadeu"), "tadeu", firstSeenAt);
    }

    private static ItemBuilder icon(Menu menu, int slot) {
        return menu.button(slot).map(MenuButton::icon).orElseThrow();
    }
}
