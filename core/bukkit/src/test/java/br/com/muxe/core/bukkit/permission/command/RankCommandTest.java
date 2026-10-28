package br.com.muxe.core.bukkit.permission.command;

import static br.com.muxe.core.bukkit.permission.command.RankTestKit.MESSAGES;
import static br.com.muxe.core.bukkit.permission.command.RankTestKit.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RankCommandTest {

    private final RankTestKit kit = new RankTestKit();
    private final FakeSender staff = FakeSender.named("Staff", RankCommand.PERMISSION);

    @BeforeEach
    void register() {
        RankReports reports = new RankReports(MESSAGES, kit.sync::data, kit.clock, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo")));
        kit.framework.register(new RankCommand(kit, kit.targets, kit.replies, reports));
        kit.online("Ana");
    }

    @AfterEach
    void close() {
        kit.close();
    }

    @Test
    void ranksAreGivenForGoodOrForATime() {
        run("cargo dar Ana vip", "cargo dar Ana vip 30d", "cargo dar Ana ultra 30d", "cargo dar Ana membro", "cargo dar Ana dragao", "cargo dar Ana mvp logo");

        assertThat(staff.messages()).containsExactly(
            "§aCargo §aVIP §adado para §fAna §7(Vitalício)§a.",
            "§fAna §cjá possui o cargo §aVIP §cpara sempre.",
            "§aCargo §dUltra §adado para §fAna §7(30d)§a.",
            "§cTodo jogador já é §7Membro§c.",
            "§cO cargo §fdragao §cnão existe. Use §f/cargo listar§c.",
            "§cTempo inválido: §flogo§c. Use §f30d§c, §f12h§c, §f1d12h §cou §fpermanente§c."
        );
        assertThat(kit.stored("Ana").nodes()).extracting(PermissionNode::key).containsExactlyInAnyOrder("group.vip", "group.ultra");
        assertThat(kit.stored("Ana").nodes()).filteredOn(node -> node.key().equals("group.ultra"))
            .singleElement().satisfies(node -> assertThat(node.expiresAt()).contains(NOW.plus(Duration.ofDays(30))));
    }

    @Test
    void ranksAreRemovedOnce() {
        run("cargo dar Ana vip");
        staff.clear();

        run("cargo remover Ana vip", "cargo remover Ana vip", "cargo remover Ninguem vip");

        assertThat(staff.messages()).containsExactly(
            "§aCargo §aVIP §aremovido de §fAna§a.",
            "§fAna §cnão possui o cargo §aVIP§c.",
            "§cO jogador §fNinguem §cnunca entrou no servidor."
        );
        assertThat(kit.stored("Ana").nodes()).isEmpty();
    }

    @Test
    void promotingAndDemotingWalkTheTrack() {
        run("cargo promover Ana doacao", "cargo promover Ana doacao");
        assertThat(kit.stored("Ana").nodes()).extracting(PermissionNode::key).containsExactly("group.mvp");

        run("cargo rebaixar Ana doacao", "cargo rebaixar Ana doacao", "cargo rebaixar Ana doacao", "cargo promover Ana nada");

        assertThat(staff.messages()).containsExactly(
            "§fAna §afoi promovido para §aVIP§a.",
            "§fAna §afoi promovido para §9MVP§a.",
            "§fAna §efoi rebaixado para §aVIP§e.",
            "§fAna §efoi rebaixado para §7Membro§e.",
            "§fAna §cnão tem cargo da trilha §fdoacao §cpara perder.",
            "§cA trilha §fnada §cnão existe. Use §f/cargo listar§c."
        );
        assertThat(kit.stored("Ana").nodes()).isEmpty();
    }

    @Test
    void viewShowsTheRanksWithTheirTimeLeft() {
        kit.online("tadeu",
            PermissionNode.group("vip", Optional.empty(), NOW),
            PermissionNode.group("ultra", Optional.of(NOW.plus(Duration.ofDays(10))), NOW));

        run("cargo ver tadeu", "cargo ver Ana");

        assertThat(staff.messages()).containsExactly(
            "§6§lCARGOS §8» §ftadeu",
            "§7Cargo principal: §dUltra",
            "§7Tag em uso: §dUltra",
            "§8• §aVIP §8- §fVitalício §8(desde 28/10/2026 12:00)",
            "§8• §dUltra §8- §f10d §8(desde 28/10/2026 12:00)",
            "§6§lCARGOS §8» §fAna",
            "§7Cargo principal: §7Membro",
            "§7Tag em uso: §7Membro",
            "§8• §7Nenhum cargo além de §7Membro§7."
        );
    }

    @Test
    void listAndInfoDescribeTheHierarchy() {
        run("cargo listar", "cargo info mvp");

        assertThat(staff.messages()).first().isEqualTo("§6§lCARGOS §8» §7do maior para o menor");
        assertThat(staff.messages()).contains(
            "§8• §cAdmin §8(admin) §7peso §f150 §8- §7herda §5Mod+",
            "§8• §7Membro §8(membro) §7peso §f0 §8- §7herda §7nenhum",
            "§8• §fdoacao§8: §7Membro §8→ §aVIP §8→ §9MVP §8→ §6Pro §8→ §dUltra",
            "§6§lCARGO §8» §9MVP §8(mvp)",
            "§7Peso: §f20",
            "§7Tag: §9MVP §8(no chat: §9§lMVP §9Steve§8)",
            "§7Herda: §aVIP",
            "§7Permissões §8(0)§7:",
            "§8• §7Nenhuma permissão."
        );
    }

    @Test
    void onlyStaffUseItAndEverythingCompletes() {
        FakeSender player = FakeSender.named("Steve");

        kit.framework.run(player.player(), "cargo ver Ana");
        kit.framework.run(staff.player(), "cargo");

        assertThat(player.messages()).containsExactly(MESSAGES.get("no-permission"));
        assertThat(staff.messages()).containsExactlyElementsOf(MESSAGES.lines("rank-usage"));
        assertThat(kit.framework.complete(staff.player(), "cargo ")).containsExactly("dar", "info", "listar", "promover", "rebaixar", "remover", "ver");
        assertThat(kit.framework.complete(staff.player(), "cargo dar Ana m")).containsExactly("membro", "mod", "mod+", "mvp");
        assertThat(kit.framework.complete(staff.player(), "cargo promover Ana ")).containsExactly("doacao", "staff");
        assertThat(kit.framework.complete(player.player(), "cargo ")).isEmpty();
    }

    private void run(String... lines) {
        for (String line : lines) {
            kit.framework.run(staff.player(), line);
        }
    }
}
