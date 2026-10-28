package br.com.muxe.core.bukkit.permission.command;

import static br.com.muxe.core.bukkit.permission.command.RankTestKit.MESSAGES;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Lifetime;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PermissionCommandTest {

    private final RankTestKit kit = new RankTestKit();
    private final FakeSender staff = FakeSender.named("Staff", PermissionCommand.PERMISSION);

    @BeforeEach
    void register() {
        RankReports reports = new RankReports(MESSAGES, kit.sync::data, kit.clock, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo")));
        kit.framework.register(new PermissionCommand(kit, kit.targets, kit.replies, reports));
        kit.online("Ana");
    }

    @AfterEach
    void close() {
        kit.close();
    }

    @Test
    void optionsComeInAnyOrder() {
        NodeOptions options = NodeOptions.parse(List.of(Optional.of("server=BedWars"), Optional.of("7d"), Optional.of("false"), Optional.empty()))
            .options().orElseThrow();

        assertThat(options).isEqualTo(new NodeOptions(false, Lifetime.of(Duration.ofDays(7)), Contexts.of("server", "bedwars")));
        assertThat(NodeOptions.parse(List.of(Optional.of("talvez"))).rejected()).contains("talvez");
        assertThat(NodeOptions.parse(List.of(Optional.of("a=b;c"))).options()).isEmpty();
        assertThat(NodeOptions.contexts(List.of(Optional.of("world=arena"), Optional.empty()))).contains(Contexts.of("world", "arena"));
        assertThat(NodeOptions.contexts(List.of(Optional.of("30d")))).isEmpty();
    }

    @Test
    void playerPermissionsAreSetListedTestedAndRemoved() {
        run(
            "permissao jogador definir Ana muxe.fly",
            "permissao jogador definir Ana muxe.kit false 7d server=bedwars",
            "permissao jogador ver Ana",
            "permissao jogador testar Ana muxe.fly",
            "permissao jogador testar Ana muxe.nada",
            "permissao jogador remover Ana muxe.kit server=bedwars",
            "permissao jogador remover Ana muxe.kit server=bedwars"
        );

        assertThat(staff.messages()).containsExactly(
            "§aPermissão §fmuxe.fly §8= §atrue §adefinida para §fAna§a.",
            "§aPermissão §fmuxe.kit §8= §cfalse §8[§7server=bedwars§8] §7(expira em 7d) §adefinida para §fAna§a.",
            "§6§lPERMISSÕES §8» §fAna §8(2)",
            "§8• §fmuxe.fly §8= §atrue",
            "§8• §fmuxe.kit §8= §cfalse §8[§7server=bedwars§8] §7(expira em 7d)",
            "§fAna §8» §fmuxe.fly§8: §aPermitido",
            "§8Testado com o servidor e o mundo onde o jogador está.",
            "§fAna §8» §fmuxe.nada§8: §7Não definido §8(vale o padrão do servidor)",
            "§8Testado com o servidor e o mundo onde o jogador está.",
            "§aPermissão §fmuxe.kit §8[§7server=bedwars§8] §aremovida de §fAna§a.",
            "§fAna §cnão tem a permissão §fmuxe.kit §8[§7server=bedwars§8] §cdefinida."
        );
        assertThat(kit.stored("Ana").nodes()).extracting(PermissionNode::key).containsExactly("muxe.fly");
        assertThat(kit.stored("Ana").nodes().getFirst().expiresAt()).isEmpty();

        kit.account("Bob");
        staff.clear();
        run("permissao jogador testar Bob muxe.fly");

        assertThat(staff.messages()).containsExactly(
            "§fBob §8» §fmuxe.fly§8: §7Não definido §8(vale o padrão do servidor)",
            "§8Jogador fora deste servidor: testado sem contexto."
        );
    }

    @Test
    void wrongPermissionsAndOptionsAreExplained() {
        run("permissao jogador definir Ana Muxe Fly", "permissao jogador definir Ana muxe/fly", "permissao jogador definir Ana muxe.fly talvez");

        assertThat(staff.messages()).containsExactly(
            "§cOpção inválida: §fFly§c. Use §ftrue§c/§ffalse§c, um tempo como §f30d §cou um contexto como §fserver=lobby§c.",
            "§cPermissão inválida: §fmuxe/fly§c. Use letras minúsculas, números e §f. _ - + *§c.",
            "§cOpção inválida: §ftalvez§c. Use §ftrue§c/§ffalse§c, um tempo como §f30d §cou um contexto como §fserver=lobby§c."
        );
        assertThat(kit.stored("Ana").nodes()).isEmpty();
    }

    @Test
    void rankPermissionsReachTheirMembersAndKeepTheRules() {
        run(
            "permissao cargo definir vip muxe.lobby.fly",
            "permissao cargo definir vip group.ultra",
            "permissao cargo remover vip muxe.lobby.fly",
            "permissao cargo remover vip muxe.lobby.fly",
            "permissao cargo definir nada muxe.fly"
        );

        assertThat(staff.messages().get(1)).startsWith("§cAlteração recusada: §fGroup '").endsWith("' ends up inheriting itself§c.");
        assertThat(staff.messages()).hasSize(5).containsSubsequence(
            "§aPermissão §fmuxe.lobby.fly §8= §atrue §adefinida no cargo §aVIP§a.",
            "§aPermissão §fmuxe.lobby.fly §aremovida do cargo §aVIP§a.",
            "§cO cargo §aVIP §cnão tem a permissão §fmuxe.lobby.fly§c.",
            "§cO cargo §fnada §cnão existe. Use §f/cargo listar§c."
        );
        assertThat(kit.sync.data().group("vip").orElseThrow().nodes()).extracting(PermissionNode::key)
            .containsExactlyInAnyOrder("group.membro", "muxe.medal.carinha", "muxe.medal.toxico", "muxe.medal.yin-yang");
        assertThat(kit.database.recentActions(10)).allSatisfy(action -> assertThat(action.actor()).isEqualTo("Staff"));
    }

    @Test
    void permissionsAndOptionsComplete() {
        assertThat(kit.framework.complete(staff.player(), "permissao ")).containsExactly("cargo", "jogador");
        assertThat(kit.framework.complete(staff.player(), "permissao jogador ")).containsExactly("definir", "remover", "testar", "ver");
        assertThat(kit.framework.complete(staff.player(), "permissao jogador definir Ana muxe.b")).containsExactly("muxe.build");
        assertThat(kit.framework.complete(staff.player(), "permissao jogador definir Ana group.v")).containsExactly("group.vip");
        assertThat(kit.framework.complete(staff.player(), "permissao jogador definir Ana muxe.fly ser")).containsExactly("server=bedwars", "server=lobby");
        assertThat(kit.framework.complete(staff.player(), "permissao jogador definir Ana muxe.fly false f")).containsExactly("false");
    }

    private void run(String... lines) {
        for (String line : lines) {
            kit.framework.run(staff.player(), line);
        }
    }
}
