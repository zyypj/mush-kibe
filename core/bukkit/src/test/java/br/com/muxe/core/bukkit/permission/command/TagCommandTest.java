package br.com.muxe.core.bukkit.permission.command;

import static br.com.muxe.core.bukkit.permission.command.RankTestKit.MESSAGES;
import static br.com.muxe.core.bukkit.permission.command.RankTestKit.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagCommandTest {

    private final RankTestKit kit = new RankTestKit();
    private final TagList list = new TagList(MESSAGES, RankTestKit.CHAT);

    @BeforeEach
    void register() {
        kit.framework.register(new TagCommand(kit, kit.targets, kit.replies, list));
    }

    @AfterEach
    void close() {
        kit.close();
    }

    @Test
    void aPlayerSelectsATagTheyCanUse() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");

        kit.framework.run(tadeu.player(), "tag VIP");

        assertThat(tadeu.messages()).containsExactly("§aVocê selecionou a tag §aVIP §acom sucesso.");
        assertThat(kit.stored("tadeu").selectedTag()).contains("vip");
        assertThat(kit.display(RankTestKit.uuid("tadeu")).formatName("tadeu")).isEqualTo("&a&lVIP &atadeu");
    }

    @Test
    void lockedAndUnknownTagsAreRefused() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");

        kit.framework.run(tadeu.player(), "tag natal");
        kit.framework.run(tadeu.player(), "tag dragao");

        assertThat(tadeu.messages()).containsExactly("§cVocê não possui a tag §cNatal§c.", "§cA tag §fdragao §cnão existe.");
        assertThat(kit.stored("tadeu").selectedTag()).isEmpty();
    }

    @Test
    void theListShowsTheUsableTagsFromTheLowestWithAPreviewAndAClick() {
        kit.online("tadeu", PermissionNode.group("ultra", Optional.empty(), NOW), PermissionNode.permanent("muxe.tag.natal", true, NOW));

        List<TagChoice> choices = list.choices(kit.resolved(RankTestKit.uuid("tadeu")).orElseThrow().tags(), "tadeu", Optional.empty());
        BaseComponent[] message = list.components(choices);

        assertThat(choices).extracting(TagChoice::label).containsExactly("§7Membro", "§aVIP", "§9MVP", "§6Pro", "§dUltra", "§cNatal");
        assertThat(choices.get(1).hover()).containsExactly("§fPrévia:", "§a§lVIP §atadeu: §fOlá!", " ", "§eClique para selecionar!");
        assertThat(choices.getFirst().hover().get(1)).isEqualTo("§7tadeu: §fOlá!");
        assertThat(TextComponent.toLegacyText(message)).isEqualTo(
            "§aSuas tags: §7Membro§f, §aVIP§f, §9MVP§f, §6Pro§f, §dUltra§f, §cNatal");
        assertThat(clicks(message)).contains("/tag membro", "/tag natal");
    }

    @Test
    void theTagPreviewShowsTheChosenMedal() {
        kit.online("tadeu", PermissionNode.group("ultra", Optional.empty(), NOW));

        List<TagChoice> choices = list.choices(kit.resolved(RankTestKit.uuid("tadeu")).orElseThrow().tags(), "tadeu", kit.data().medal("cafe"));

        assertThat(choices.get(1).hover().get(1)).isEqualTo("§e☕ §a§lVIP §atadeu: §fOlá!");
    }

    @Test
    void staffGiveAndRemoveDecorativeTagsWithAnOptionalTime() {
        kit.online("Ana");
        FakeSender staff = FakeSender.named("Staff", TagCommand.MANAGE_PERMISSION);

        kit.framework.run(staff.player(), "tag dar Ana natal 30d");
        kit.framework.run(staff.player(), "tag dar Ana natal 2d");
        kit.framework.run(staff.player(), "tag dar Ana vip");
        kit.framework.run(staff.player(), "tag dar Ana natal amanha");
        kit.framework.run(staff.player(), "tag dar Ninguem natal");

        assertThat(staff.messages()).containsExactly(
            "§aTag §cNatal §adada para §fAna §7(30d)§a.",
            "§aTag §cNatal §adada para §fAna §7(2d)§a.",
            "§cA tag §aVIP §cvem de um cargo. Use §f/cargo dar§c.",
            "§cTempo inválido: §famanha§c. Use §f30d§c, §f12h§c, §f1d12h §cou §fpermanente§c.",
            "§cO jogador §fNinguem §cnunca entrou no servidor."
        );
        assertThat(kit.stored("Ana").nodes()).singleElement().satisfies(node -> {
            assertThat(node.key()).isEqualTo("muxe.tag.natal");
            assertThat(node.expiresAt()).contains(NOW.plus(Duration.ofDays(32)));
        });
        assertThat(kit.database.recentActions(5)).extracting(PermissionAction::actor).containsOnly("Staff");

        staff.clear();
        kit.framework.run(staff.player(), "tag remover Ana natal");
        kit.framework.run(staff.player(), "tag remover Ana natal");

        assertThat(staff.messages()).containsExactly("§aTag §cNatal §aremovida de §fAna§a.", "§fAna §cnão possui a tag §cNatal§c.");
        assertThat(kit.stored("Ana").nodes()).isEmpty();
    }

    @Test
    void onlyStaffManageTagsAndSeeTheFullUsage() {
        FakeSender player = FakeSender.named("Steve");
        FakeSender staff = FakeSender.named("Staff", TagCommand.MANAGE_PERMISSION);

        kit.framework.run(player.player(), "tag dar Ana natal");
        kit.framework.run(player.player(), "tag a b");
        kit.framework.run(staff.player(), "tag a b");

        assertThat(player.messages()).containsExactly(MESSAGES.get("no-permission"), "§cUso: §f/tag [tag]");
        assertThat(staff.messages()).containsExactlyElementsOf(MESSAGES.lines("tag-usage-staff"));
    }

    @Test
    void tabCompletesUsableTagsForPlayersAndDecorativeTagsForStaff() {
        kit.online("tadeu", PermissionNode.group("mvp", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");
        FakeSender staff = FakeSender.named("Staff", TagCommand.MANAGE_PERMISSION);

        assertThat(kit.framework.complete(tadeu.player(), "tag ")).containsExactly("membro", "mvp", "vip");
        assertThat(kit.framework.complete(staff.player(), "tag d")).containsExactly("dar");
        assertThat(kit.framework.complete(staff.player(), "tag dar A")).containsExactly("Ana");
        assertThat(kit.framework.complete(staff.player(), "tag dar Ana n")).containsExactly("natal");
        assertThat(kit.framework.complete(staff.player(), "tag dar Ana natal 3")).containsExactly("30d", "365d");
    }

    private static List<String> clicks(BaseComponent[] message) {
        return List.of(message).stream()
            .map(BaseComponent::getClickEvent)
            .filter(event -> event != null && event.getAction() == ClickEvent.Action.RUN_COMMAND)
            .map(ClickEvent::getValue)
            .distinct()
            .toList();
    }
}
