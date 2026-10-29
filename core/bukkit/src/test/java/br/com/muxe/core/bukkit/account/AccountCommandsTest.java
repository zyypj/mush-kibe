package br.com.muxe.core.bukkit.account;

import static br.com.muxe.core.bukkit.permission.command.RankTestKit.MESSAGES;
import static br.com.muxe.core.bukkit.permission.command.RankTestKit.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.permission.command.RankTestKit;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountCommandsTest {

    private static final Instant VIP_AT = Instant.parse("2022-09-09T17:09:00Z");
    private static final Instant ULTRA_AT = Instant.parse("2023-10-21T14:29:00Z");

    private final RankTestKit kit = new RankTestKit();
    private final AccountSettings settings = AccountSettings.from(ShopFixtures.CONFIG.getConfigurationSection("menus.account"));
    private final AccountLines lines = new AccountLines(MESSAGES, settings.chatDates());
    private final AccountMenu menu = new AccountMenu(settings, lines, MESSAGES);
    private final AccountProfiles profiles = new AccountProfiles(kit.queries, kit);

    @BeforeEach
    void register() {
        kit.framework.register(new AccountCommands(profiles, lines, menu, kit.replies));
        kit.online("tadeu",
            new PermissionNode("group.vip", true, Contexts.NONE, Optional.empty(), VIP_AT, VIP_AT),
            new PermissionNode("group.ultra", true, Contexts.NONE, Optional.of(NOW.plus(Duration.ofDays(10))), ULTRA_AT, ULTRA_AT.plusSeconds(60)));
        kit.details.saveLocation(RankTestKit.uuid("tadeu"), new AccountLocation(
            Optional.of("Brasil"), Optional.of("Minas Gerais"), Optional.of("Tupaciguara"), Optional.of("AS264207 Ponto Net Telecom")), NOW);
        kit.online("Ana");
    }

    @AfterEach
    void close() {
        kit.close();
    }

    @Test
    void accountListsTheUserTheirRanksLoginsAndLocation() {
        FakeSender tadeu = FakeSender.named("tadeu");

        kit.framework.run(tadeu.player(), "account");

        assertThat(tadeu.messages()).containsExactly(
            "§aUsuário: §ftadeu",
            "§aTipo: §fPremium",
            "§aDiscord: §fEm breve",
            "§aRank: §fVIP",
            "  §7Adicionado em: 09/09/2022 14:09",
            "  §7Atualizado em: 09/09/2022 14:09",
            "§aRank: §fUltra",
            "  §7Adicionado em: 21/10/2023 11:29",
            "  §7Atualizado em: 21/10/2023 11:30",
            "  §7Expira em: 07/11/2026 12:00 (10d)",
            "§aPrimeiro login: §f27/10/2026 12:00",
            "§aÚltimo login: §f27/10/2026 12:00",
            "§aLocalização:",
            "  §7País: Brasil",
            "  §7Estado: Minas Gerais",
            "  §7Cidade: Tupaciguara",
            "  §7ASN: AS264207 Ponto Net Telecom"
        );
    }

    @Test
    void playersWithoutRanksOrLocationShowMemberAndUnknown() {
        FakeSender ana = FakeSender.named("Ana");

        kit.framework.run(ana.player(), "conta");

        assertThat(ana.messages()).contains("§aRank: §fMembro", "§aLocalização:", "  §7Desconhecida").doesNotContain("  §7País: Brasil");
        assertThat(ana.messages()).noneMatch(line -> line.contains("Adicionado em"));
    }

    @Test
    void otherPlayersNeedThePermission() {
        FakeSender ana = FakeSender.named("Ana");
        FakeSender staff = FakeSender.named("Staff", AccountCommands.OTHERS_PERMISSION);
        FakeSender console = FakeSender.named("CONSOLE");

        kit.framework.run(ana.player(), "account tadeu");
        kit.framework.run(ana.player(), "account ana");
        kit.framework.run(staff.player(), "account Ninguem");
        kit.framework.run(console.console(), "account");

        assertThat(ana.messages()).first().isEqualTo(MESSAGES.get("no-permission"));
        assertThat(ana.messages()).contains("§aUsuário: §fAna");
        assertThat(staff.messages()).containsExactly("§cO jogador §fNinguem §cnunca entrou no servidor.");
        assertThat(console.messages()).containsExactly("§cUso: §f/account <jogador>");
    }

    @Test
    void theMenuShowsTheHeadOfTheAccountAndACloseBed() {
        AccountProfile profile = profiles.find(FindAccountDetailsQuery.byUsername("tadeu"), "tadeu").join().orElseThrow();

        Menu player = menu.menu(profile, false);
        Menu staff = menu.menu(profile, true);

        ItemBuilder head = icon(player, 4);
        assertThat(player.title()).isEqualTo("Conta: tadeu");
        assertThat(player.rows()).isEqualTo(6);
        assertThat(head.skullOwner()).isEqualTo("tadeu");
        assertThat(head.name()).isEqualTo("§dtadeu");
        assertThat(head.lore()).containsExactly(
            "§7Tipo: §fPremium",
            "§7ID: §f(1)",
            "§7ID de Perfil: §fEm breve",
            "§7UUID: §f" + RankTestKit.uuid("tadeu"),
            " ",
            "§7Rank: §aVIP",
            "§7Rank: §dUltra",
            "§7Primeiro login: §f27/10/2026 12:00:00",
            "§7Último login: §f27/10/2026 12:00:00",
            "§7Discord: §fEm breve"
        );
        assertThat(icon(player, 49).material()).isEqualTo(Material.BED);
        assertThat(icon(player, 49).name()).isEqualTo("§cFechar");
        assertThat(player.buttons().keySet()).containsExactlyInAnyOrder(4, 49);
        assertThat(staff.buttons().keySet()).containsExactlyInAnyOrder(4, 20, 21, 23, 24, 49);
        assertThat(icon(staff, 20).name()).isEqualTo("§aPunições");
    }

    private static ItemBuilder icon(Menu menu, int slot) {
        return menu.button(slot).map(MenuButton::icon).orElseThrow();
    }
}
