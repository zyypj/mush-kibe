package br.com.muxe.core.bukkit.party.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.command.framework.TestCommands;
import br.com.muxe.core.bukkit.party.ChannelChoices;
import br.com.muxe.core.bukkit.party.NetworkPlayers;
import br.com.muxe.core.bukkit.party.PartyChat;
import br.com.muxe.core.bukkit.party.PartyChatListener;
import br.com.muxe.core.bukkit.party.PartyClient;
import br.com.muxe.core.bukkit.party.PartyTexts;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.chat.ChatChannel;
import br.com.muxe.core.common.chat.ChatChannels;
import br.com.muxe.core.common.network.party.PartyAction;
import br.com.muxe.core.common.network.party.PartyActionPacket;
import br.com.muxe.core.common.network.party.PartyNoticePacket;
import br.com.muxe.core.common.network.party.PartySyncRequest;
import br.com.muxe.core.common.network.party.PartySyncResponse;
import br.com.muxe.core.common.network.player.OnlinePlayersPacket;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.TestNetwork;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartyCommandsTest {

    private static final MessageCatalog MESSAGES = coreMessages();

    private final TestNetwork network = new TestNetwork();
    private final TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
    private final TestNetwork.Node lobby = network.node("lobby-1", "lobby", ServerKind.LOBBY);
    private final List<PartyActionPacket> actions = new CopyOnWriteArrayList<>();
    private final FakeSender tadeu = FakeSender.named("tadeu");
    private final FakeSender dobw = FakeSender.named("tadeudobw");
    private final FakeSender leader = FakeSender.named("Lider", PartyCommand.OPEN_PERMISSION);
    private List<PartySnapshot> parties = List.of();
    private PartyClient client;
    private ChannelChoices channels;
    private PartyChat chat;
    private TestCommands commands;

    @BeforeEach
    void setUp() {
        proxy.bus().subscribe(PartyActionPacket.TYPE, context -> actions.add(context.packet()));
        proxy.bus().subscribe(PartySyncRequest.TYPE, context -> context.reply(new PartySyncResponse(parties)));
        proxy.start();
        lobby.start();
        Map<UUID, Player> online = Map.of(id(tadeu), tadeu.player(), id(dobw), dobw.player());
        client = new PartyClient(lobby.bus(), Runnable::run, "lobby-1", Duration.ofSeconds(1),
            new PartyTexts(MESSAGES, "{medal}{prefix}{color}{player}: &f{message}"), id -> Optional.ofNullable(online.get(id)),
            new NetworkPlayers(() -> List.of("tadeu")), Logger.getLogger("test"));
        client.start();
        chat = new PartyChat(client, id -> RankDisplay.NONE);
        channels = new ChannelChoices(new ChatChannels(new InMemoryExpiringStore(network.clock()), ChatChannels.DEFAULT_DURATION), Runnable::run);
        commands = TestCommands.create();
        commands.commands().suggestions(NetworkPlayers.SUGGESTIONS, sender -> client.networkPlayers().namesExcept(sender.getName()));
        commands.commands().suggestions(PartyCommand.MEMBERS, sender -> PartyCommand.members(client, sender));
        commands.commands().suggestions(PartyCommand.OPEN_PARTIES, sender -> PartyCommand.openParties(client));
        commands.register(new PartyCommand(client), new PartyChatCommand(chat, MESSAGES), new ChannelCommand(channels, chat, MESSAGES));
    }

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void invitingSeveralPlayersSendsEachNameOnce() {
        commands.run(tadeu.player(), "party Ana bia ana");

        assertThat(actions).singleElement().satisfies(action -> {
            assertThat(action.action()).isEqualTo(PartyAction.INVITE);
            assertThat(action.actor()).isEqualTo(id(tadeu));
            assertThat(action.actorName()).isEqualTo("tadeu");
            assertThat(action.arguments()).containsExactly("Ana", "bia");
        });
    }

    @Test
    void everySubcommandReachesTheProxy() {
        commands.run(tadeu.player(), "party aceitar Lider");
        commands.run(tadeu.player(), "party sair");
        commands.run(tadeu.player(), "party acabar");
        commands.run(tadeu.player(), "party expulsar ana");
        commands.run(tadeu.player(), "party transferir ana");
        commands.run(tadeu.player(), "party teleportar");
        commands.run(tadeu.player(), "party warp");
        commands.run(tadeu.player(), "party entrar Lider");
        commands.run(tadeu.player(), "party removeroffline");

        assertThat(actions).extracting(PartyActionPacket::action).containsExactly(PartyAction.ACCEPT, PartyAction.LEAVE, PartyAction.DISBAND,
            PartyAction.KICK, PartyAction.TRANSFER, PartyAction.TELEPORT, PartyAction.TELEPORT, PartyAction.JOIN, PartyAction.REMOVE_OFFLINE);
        assertThat(actions.getFirst().argument()).isEqualTo("Lider");
        assertThat(actions.get(3).argument()).isEqualTo("ana");
    }

    @Test
    void openingCarriesWhetherThePlayerHasUltraPlus() {
        commands.run(tadeu.player(), "party abrir");
        commands.run(leader.player(), "party abrir");

        assertThat(actions).extracting(PartyActionPacket::allowed).containsExactly(false, true);
    }

    @Test
    void theUsageListsEverySubcommandAsInTheReference() {
        commands.run(tadeu.player(), "party");

        assertThat(tadeu.messages()).hasSize(13);
        assertThat(tadeu.messages().getFirst()).isEqualTo("§6Uso §a/party§6:");
        assertThat(tadeu.messages().get(1)).isEqualTo("§e/party <jogadores> - §bConvidar para party");
        assertThat(tadeu.messages().getLast()).isEqualTo("§e/canal party - §bAltera o seu canal padrão de chat para a party");
    }

    @Test
    void membersAreListedOnlineInGreenAndOfflineInRed() {
        syncParty(false);

        commands.run(tadeu.player(), "party membros");

        assertThat(tadeu.messages()).containsExactly("§eMembros da party §7(3)§e: §atadeu§7, §atadeudobw§7, §cana");
    }

    @Test
    void membersOutOfAPartyAreTold() {
        commands.run(tadeu.player(), "party membros");

        assertThat(tadeu.messages()).containsExactly("§cVocê não está em uma party.");
    }

    @Test
    void tabCompletesSubcommandsPlayersMembersAndOpenParties() {
        syncParty(true);
        proxy.bus().publish(PacketTarget.broadcast(), new OnlinePlayersPacket(List.of("Ana", "Bia", "tadeu"))).join();

        assertThat(commands.complete(tadeu.player(), "party ")).contains("aceitar", "abrir", "membros", "teleportar", "Ana", "Bia").doesNotContain("tadeu");
        assertThat(commands.complete(tadeu.player(), "party Ana B")).containsExactly("Bia");
        assertThat(commands.complete(tadeu.player(), "party expulsar ")).containsExactly("ana", "tadeudobw");
        assertThat(commands.complete(tadeu.player(), "party entrar ")).containsExactly("tadeu");
        assertThat(commands.complete(tadeu.player(), "party w")).containsExactly("warp");
        assertThat(commands.complete(tadeu.player(), "canal ")).containsExactly("clan", "default", "party");
    }

    @Test
    void partyChatNeedsAPartyAndUsesTheChatFormat() {
        commands.run(tadeu.player(), "pc oi");
        assertThat(tadeu.messages()).containsExactly("§cVocê não está em uma party.");
        syncParty(false);

        commands.run(tadeu.player(), "pc oi &a 100%");
        commands.run(tadeu.player(), "pc");

        assertThat(actions).singleElement().satisfies(action -> {
            assertThat(action.action()).isEqualTo(PartyAction.CHAT);
            assertThat(action.argument()).isEqualTo("§d[PARTY] §7tadeu: §foi &a 100%");
        });
        assertThat(tadeu.messages()).endsWith("§cUso /pc <mensagem>");
    }

    @Test
    void theChannelCommandChangesTheDefaultChannel() {
        commands.run(tadeu.player(), "canal party");
        commands.run(tadeu.player(), "canal clan");
        commands.run(tadeu.player(), "canal global");
        commands.run(tadeu.player(), "canal");
        syncParty(false);
        commands.run(tadeu.player(), "canal party");
        assertThat(channels.of(id(tadeu))).isEqualTo(ChatChannel.PARTY);
        commands.run(tadeu.player(), "canal padrão");

        assertThat(tadeu.messages()).containsExactly(
            "§cVocê não está em uma party.",
            "§cVocê não está em um clan.",
            "§cUso /canal <canal>",
            "§cUso /canal <canal>",
            "§eSeu canal de chat agora é §bparty§e.",
            "§eSeu canal de chat agora é §bpadrão§e."
        );
        assertThat(channels.of(id(tadeu))).isEqualTo(ChatChannel.DEFAULT);
    }

    @Test
    void inThePartyChannelTheChatGoesToTheParty() {
        PartyChatListener listener = new PartyChatListener(channels, chat);
        AsyncPlayerChatEvent outside = new AsyncPlayerChatEvent(false, tadeu.player(), "oi", new HashSet<>());
        listener.onChat(outside);
        assertThat(outside.isCancelled()).isFalse();

        syncParty(false);
        channels.choose(id(tadeu), ChatChannel.PARTY);
        AsyncPlayerChatEvent inside = new AsyncPlayerChatEvent(false, tadeu.player(), "oi", new HashSet<>());
        listener.onChat(inside);

        assertThat(inside.isCancelled()).isTrue();
        assertThat(actions).extracting(PartyActionPacket::argument).containsExactly("§d[PARTY] §7tadeu: §foi");
    }

    @Test
    void withoutAPartyThePartyChannelFallsBackToTheDefaultChat() {
        channels.choose(id(tadeu), ChatChannel.PARTY);
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, tadeu.player(), "oi", new HashSet<>());

        new PartyChatListener(channels, chat).onChat(event);

        assertThat(event.isCancelled()).isFalse();
        assertThat(actions).isEmpty();
    }

    @Test
    void noticesFromTheProxyReachThePlayersOfThisServerWithTheirColours() {
        PartyNotice notice = PartyNotice.of(List.of(id(tadeu), id(dobw), UUID.randomUUID()), "party-kicked", "leader", "&5tadeu", "player", "&7ana");

        proxy.bus().publish(PacketTarget.server("lobby-1"), new PartyNoticePacket(notice)).join();

        assertThat(tadeu.messages()).containsExactly("§d[PARTY] §5tadeu §cremoveu §7ana §cda party.");
        assertThat(dobw.messages()).containsExactly("§d[PARTY] §5tadeu §cremoveu §7ana §cda party.");
    }

    @Test
    void theInviteHasTwoLinesAndTheLastOneAccepts() {
        PartyNotice invite = PartyNotice.of(List.of(id(tadeu)), "party-invite", "player", "&5tadeu").clicking("/party aceitar tadeu");

        assertThat(client.texts().lines(invite)).containsExactly("§5tadeu §econvidou você para uma party", "§b§lCLIQUE AQUI §epara aceitar o convite.");
    }

    @Test
    void disconnectsAndReturnsMatchTheReference() {
        PartyNotice left = PartyNotice.of(List.of(id(tadeu)), "party-member-disconnected", "player", "&7tadeudobw", "minutes", "3");
        PartyNotice back = PartyNotice.of(List.of(id(tadeu)), "party-member-reconnected", "player", "&7tadeudobw");

        assertThat(client.texts().lines(left)).containsExactly("§d[PARTY] §7tadeudobw §cdesconectou-se e tem 3 minutos para voltar.");
        assertThat(client.texts().lines(back)).containsExactly("§d[PARTY] §7tadeudobw §ereconectou.");
    }

    @Test
    void playersNotFoundAreListedAsInTheReference() {
        PartyNotice missing = PartyNotice.of(List.of(id(tadeu)), "party-player-not-found", "names", "_mmike_");

        assertThat(client.texts().lines(missing)).containsExactly("§cJogadores: _mmike_ não foi encontrado ou está com o convite de party desativado.");
    }

    @Test
    void rawPartyChatIsShownAsItCame() {
        client.deliver(PartyNotice.raw(List.of(id(dobw)), "§d[PARTY] §7tadeu: §f&aoi"));

        assertThat(dobw.messages()).containsExactly("§d[PARTY] §7tadeu: §f&aoi");
    }

    private void syncParty(boolean open) {
        parties = List.of(new PartySnapshot(UUID.randomUUID(), id(tadeu), open, List.of(
            new PartySnapshot.Member(id(tadeu), "tadeu", true),
            new PartySnapshot.Member(id(dobw), "tadeudobw", true),
            new PartySnapshot.Member(UUID.randomUUID(), "ana", false))));
        client.sync();
        assertThat(client.of(id(tadeu))).isPresent();
    }

    private static UUID id(FakeSender sender) {
        return sender.player().getUniqueId();
    }

    private static MessageCatalog coreMessages() {
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(PartyCommandsTest.class.getResourceAsStream("/config.yml")),
            StandardCharsets.UTF_8)) {
            return MessageCatalog.from(YamlConfiguration.loadConfiguration(reader).getConfigurationSection("messages"));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
