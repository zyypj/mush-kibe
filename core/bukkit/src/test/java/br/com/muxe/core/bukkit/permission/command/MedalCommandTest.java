package br.com.muxe.core.bukkit.permission.command;

import static br.com.muxe.core.bukkit.permission.command.RankTestKit.MESSAGES;
import static br.com.muxe.core.bukkit.permission.command.RankTestKit.NOW;
import static br.com.muxe.core.bukkit.permission.command.RankTestKit.uuid;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedalCommandTest {

    private final RankTestKit kit = new RankTestKit();
    private final MedalList list = new MedalList(MESSAGES, RankTestKit.CHAT);

    @BeforeEach
    void register() {
        kit.framework.register(new MedalCommand(kit, kit.targets, kit.replies, list));
    }

    @AfterEach
    void close() {
        kit.close();
    }

    @Test
    void theListStartsWithNoneThenTheUsableMedalsByWeightEachWithAPreviewAndAClick() {
        kit.online("tadeu", PermissionNode.group("ultra", Optional.empty(), NOW), PermissionNode.permanent("muxe.tag.enderlore", true, NOW));
        kit.sync.selectTag(uuid("tadeu"), Optional.of("enderlore")).join();

        List<MedalChoice> choices = list.choices(kit.resolved(uuid("tadeu")).orElseThrow().medals(), kit.display(uuid("tadeu")), "tadeu");
        BaseComponent[] message = list.components(choices);

        assertThat(choices).extracting(MedalChoice::label).containsExactly("§7Nenhuma", "§a☣", "§f☯", "§5♫", "§6⚡", "§c✌", "§4❤", "§e☕", "§6ツ");
        assertThat(TextComponent.toLegacyText(message)).isEqualTo(
            "§aMedalhas disponíveis: §7Nenhuma§f, §a☣§f, §f☯§f, §5♫§f, §6⚡§f, §c✌§f, §4❤§f, §e☕§f, §6ツ");
        assertThat(choices.get(7).hover()).containsExactly(
            "§eCafé", " ", "§fPrévia:", "§e☕ §5§lENDERLORE §5tadeu§f: Olá!", " ", "§eClique para selecionar!");
        assertThat(choices.getFirst().hover()).containsExactly(
            "§7Nenhuma", " ", "§fPrévia:", "§5§lENDERLORE §5tadeu§f: Olá!", " ", "§eClique para selecionar!");
        assertThat(clicks(message)).containsExactly(
            "/medalha nenhuma", "/medalha toxico", "/medalha yin-yang", "/medalha musico", "/medalha flash",
            "/medalha paz-e-amor", "/medalha amor", "/medalha cafe", "/medalha carinha");
    }

    @Test
    void aMemberOnlySeesNoneAndTheListIsSentAsOneClickableLine() {
        kit.online("tadeu");
        FakeSender tadeu = FakeSender.named("tadeu");
        List<BaseComponent[]> sent = new ArrayList<>();

        kit.framework.run(withChat(tadeu.player(), sent), "medalha");

        assertThat(tadeu.messages()).isEmpty();
        assertThat(sent).singleElement().satisfies(line -> assertThat(TextComponent.toLegacyText(line)).isEqualTo("§aMedalhas disponíveis: §7Nenhuma"));
        assertThat(clicks(sent.getFirst())).containsExactly("/medalha nenhuma");
    }

    @Test
    void aPlayerSelectsAMedalTheyCanUseAndTheChatShowsItBeforeTheTag() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");

        kit.framework.run(tadeu.player(), "medalha TOXICO");

        assertThat(tadeu.messages()).containsExactly("§aVocê selecionou a medalha §a☣ §aTóxico §acom sucesso.");
        assertThat(kit.stored("tadeu").selectedMedal()).contains("toxico");
        assertThat(String.format(kit.display(uuid("tadeu")).chatFormat(RankTestKit.CHAT), "tadeu", "oi")).isEqualTo("§a☣ §a§lVIP §atadeu§f: oi");
    }

    @Test
    void noneRemovesTheMedalFromTheChat() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");
        kit.framework.run(tadeu.player(), "medalha toxico");
        tadeu.clear();

        kit.framework.run(tadeu.player(), "medalha nenhuma");

        assertThat(tadeu.messages()).containsExactly("§aVocê removeu a sua medalha.");
        assertThat(kit.stored("tadeu").selectedMedal()).isEmpty();
        assertThat(String.format(kit.display(uuid("tadeu")).chatFormat(RankTestKit.CHAT), "tadeu", "oi")).isEqualTo("§a§lVIP §atadeu§f: oi");
    }

    @Test
    void lockedAndUnknownMedalsAreRefused() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");

        kit.framework.run(tadeu.player(), "medalha cafe");
        kit.framework.run(tadeu.player(), "medalha dragao");

        assertThat(tadeu.messages()).containsExactly("§cVocê não possui a medalha §e☕ §eCafé§c.", "§cA medalha §fdragao §cnão existe.");
        assertThat(kit.stored("tadeu").selectedMedal()).isEmpty();
    }

    @Test
    void aMedalThePlayerLosesStopsShowingButTheChoiceIsKept() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        kit.framework.run(FakeSender.named("tadeu").player(), "medalha toxico");

        kit.sync.removeNode("Staff", uuid("tadeu"), PermissionNode.GROUP_PREFIX + "vip", Contexts.NONE).join();

        assertThat(kit.display(uuid("tadeu")).medal()).isEmpty();
        assertThat(String.format(kit.display(uuid("tadeu")).chatFormat(RankTestKit.CHAT), "tadeu", "oi")).isEqualTo("§7tadeu§f: oi");
        assertThat(kit.stored("tadeu").selectedMedal()).contains("toxico");
    }

    @Test
    void staffGiveAndRemoveMedalsWithAnOptionalTime() {
        kit.online("Ana");
        FakeSender staff = FakeSender.named("Staff", MedalCommand.MANAGE_PERMISSION);

        kit.framework.run(staff.player(), "medalha dar Ana cafe 30d");
        kit.framework.run(staff.player(), "medalha dar Ana cafe 2d");
        kit.framework.run(staff.player(), "medalha dar Ana dragao");
        kit.framework.run(staff.player(), "medalha dar Ana cafe amanha");
        kit.framework.run(staff.player(), "medalha dar Ninguem cafe");

        assertThat(staff.messages()).containsExactly(
            "§aMedalha §e☕ §eCafé §adada para §fAna §7(30d)§a.",
            "§aMedalha §e☕ §eCafé §adada para §fAna §7(2d)§a.",
            "§cA medalha §fdragao §cnão existe.",
            "§cTempo inválido: §famanha§c. Use §f30d§c, §f12h§c, §f1d12h §cou §fpermanente§c.",
            "§cO jogador §fNinguem §cnunca entrou no servidor."
        );
        assertThat(kit.stored("Ana").nodes()).singleElement().satisfies(node -> {
            assertThat(node.key()).isEqualTo("muxe.medal.cafe");
            assertThat(node.expiresAt()).contains(NOW.plus(Duration.ofDays(32)));
        });
        assertThat(kit.resolved(uuid("Ana")).orElseThrow().medals()).extracting(medal -> medal.id()).containsExactly("cafe");
        assertThat(kit.database.recentActions(5)).extracting(PermissionAction::actor).containsOnly("Staff");

        staff.clear();
        kit.framework.run(staff.player(), "medalha remover Ana cafe");
        kit.framework.run(staff.player(), "medalha remover Ana cafe");

        assertThat(staff.messages()).containsExactly("§aMedalha §e☕ §eCafé §aremovida de §fAna§a.", "§fAna §cnão possui a medalha §e☕ §eCafé§c.");
        assertThat(kit.stored("Ana").nodes()).isEmpty();
    }

    @Test
    void aPermanentMedalCannotBeGivenAgain() {
        kit.online("Ana", PermissionNode.permanent("muxe.medal.natal", true, NOW));
        FakeSender staff = FakeSender.named("Staff", MedalCommand.MANAGE_PERMISSION);

        kit.framework.run(staff.player(), "medalha dar Ana natal 7d");

        assertThat(staff.messages()).containsExactly("§fAna §cjá possui a medalha §b✽ §bNatal §cpara sempre.");
    }

    @Test
    void onlyStaffManageMedalsAndSeeTheFullUsage() {
        FakeSender player = FakeSender.named("Steve");
        FakeSender staff = FakeSender.named("Staff", MedalCommand.MANAGE_PERMISSION);

        kit.framework.run(player.player(), "medalha dar Ana cafe");
        kit.framework.run(player.player(), "medalha a b");
        kit.framework.run(staff.player(), "medalha a b");

        assertThat(player.messages()).containsExactly(MESSAGES.get("no-permission"), "§cUso: §f/medalha [medalha]");
        assertThat(staff.messages()).containsExactlyElementsOf(MESSAGES.lines("medal-usage-staff"));
    }

    @Test
    void tabCompletesOwnedMedalsAndNoneForPlayersAndEveryMedalForStaff() {
        kit.online("tadeu", PermissionNode.group("vip", Optional.empty(), NOW));
        FakeSender tadeu = FakeSender.named("tadeu");
        FakeSender staff = FakeSender.named("Staff", MedalCommand.MANAGE_PERMISSION);

        assertThat(kit.framework.complete(tadeu.player(), "medalha ")).containsExactly("carinha", "nenhuma", "toxico", "yin-yang");
        assertThat(kit.framework.complete(staff.player(), "medalha d")).containsExactly("dar");
        assertThat(kit.framework.complete(staff.player(), "medalha dar A")).containsExactly("Ana");
        assertThat(kit.framework.complete(staff.player(), "medalha dar Ana ca")).containsExactly("cacador-de-bruxas", "cafe", "carinha", "carnaval", "carta", "casa-de-gengibre");
        assertThat(kit.framework.complete(staff.player(), "medalha remover Ana fl")).containsExactly("flash", "flor");
        assertThat(kit.framework.complete(staff.player(), "medalha dar Ana cafe 3")).containsExactly("30d", "365d");
    }

    private static Player withChat(Player player, List<BaseComponent[]> sent) {
        Player.Spigot spigot = new Player.Spigot() {
            @Override
            public void sendMessage(BaseComponent... components) {
                sent.add(components);
            }
        };
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
            (proxy, method, args) -> "spigot".equals(method.getName()) ? spigot : method.invoke(player, args));
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
