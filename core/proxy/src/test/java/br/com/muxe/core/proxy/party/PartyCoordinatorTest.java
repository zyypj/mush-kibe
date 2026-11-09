package br.com.muxe.core.proxy.party;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.party.PartyAction;
import br.com.muxe.core.common.network.party.PartyActionPacket;
import br.com.muxe.core.common.network.party.PartyGatherRequest;
import br.com.muxe.core.common.network.party.PartyGatherResponse;
import br.com.muxe.core.common.network.party.PartyNoticePacket;
import br.com.muxe.core.common.network.party.PartyRemovedPacket;
import br.com.muxe.core.common.network.party.PartySyncRequest;
import br.com.muxe.core.common.network.party.PartySyncResponse;
import br.com.muxe.core.common.network.party.PartyUpdatedPacket;
import br.com.muxe.core.common.network.player.OnlinePlayersPacket;
import br.com.muxe.core.common.network.player.PlayerDisplayPacket;
import br.com.muxe.core.common.party.domain.PartyBook;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartyPlayer;
import br.com.muxe.core.common.party.domain.PartyRules;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartyCoordinatorTest {

    private static final PartyPlayer TADEU = new PartyPlayer(UUID.randomUUID(), "tadeu");
    private static final PartyPlayer DOBW = new PartyPlayer(UUID.randomUUID(), "tadeudobw");

    private final TestNetwork network = new TestNetwork();
    private final TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
    private final TestNetwork.Node lobby = network.node("lobby-1", "lobby", ServerKind.LOBBY);
    private final TestNetwork.Node bedwars = network.node("bedwars-1", "bedwars", ServerKind.MINIGAME);
    private final FakeNetwork players = new FakeNetwork();
    private final List<PartyNotice> lobbyNotices = new CopyOnWriteArrayList<>();
    private final List<PartyNotice> bedwarsNotices = new CopyOnWriteArrayList<>();
    private final List<Object> broadcasts = new CopyOnWriteArrayList<>();
    private PartyCoordinator parties;

    @BeforeEach
    void setUp() {
        PlayerDisplays displays = new PlayerDisplays();
        parties = new PartyCoordinator(proxy.bus(), new PartyBook(PartyRules.DEFAULT, displays::of), displays, players, network.clock(),
            Duration.ofSeconds(1), Logger.getLogger("test"));
        parties.register();
        lobby.bus().subscribe(PartyNoticePacket.TYPE, context -> lobbyNotices.add(context.packet().notice()));
        bedwars.bus().subscribe(PartyNoticePacket.TYPE, context -> bedwarsNotices.add(context.packet().notice()));
        lobby.bus().subscribe(PartyUpdatedPacket.TYPE, context -> broadcasts.add(context.packet()));
        lobby.bus().subscribe(PartyRemovedPacket.TYPE, context -> broadcasts.add(context.packet()));
        lobby.bus().subscribe(OnlinePlayersPacket.TYPE, context -> broadcasts.add(context.packet()));
        proxy.start();
        lobby.start();
        bedwars.start();
        players.put(TADEU, "lobby-1");
        players.put(DOBW, "bedwars-1");
    }

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void anInviteReachesEachPlayerOnTheirOwnServer() {
        act(TADEU, PartyAction.INVITE, "TadeuDoBW");

        assertThat(lobbyNotices).extracting(PartyNotice::key).containsExactly("party-invite-sent");
        assertThat(bedwarsNotices).extracting(PartyNotice::key).containsExactly("party-invite");
        assertThat(bedwarsNotices.getFirst().recipients()).containsExactly(DOBW.id());
        assertThat(broadcasts).hasAtLeastOneElementOfType(PartyUpdatedPacket.class);
    }

    @Test
    void namesUseTheColourEachServerReported() {
        lobby.bus().publish(PacketTarget.proxy(), new PlayerDisplayPacket(TADEU.id(), "&5tadeu")).join();

        act(TADEU, PartyAction.INVITE, "tadeudobw");

        assertThat(bedwarsNotices.getFirst().value("player")).hasValue("&5tadeu");
        assertThat(lobbyNotices.getFirst().value("player")).hasValue("&7tadeudobw");
    }

    @Test
    void invitingSomeoneOfflineAnswersOnlyTheActor() {
        act(TADEU, PartyAction.INVITE, "ninguem");

        assertThat(lobbyNotices).singleElement().satisfies(notice -> {
            assertThat(notice.key()).isEqualTo("party-player-not-found");
            assertThat(notice.value("name")).hasValue("ninguem");
        });
        assertThat(bedwarsNotices).isEmpty();
    }

    @Test
    void severalPlayersAreInvitedAtOnce() {
        PartyPlayer ana = new PartyPlayer(UUID.randomUUID(), "ana");
        players.put(ana, "lobby-1");

        act(TADEU, PartyAction.INVITE, "tadeudobw", "ana");

        assertThat(lobbyNotices).extracting(PartyNotice::key).containsExactly("party-invite-sent", "party-invite-sent", "party-invite");
    }

    @Test
    void theOpenToggleCarriesThePermissionCheckedByTheServer() {
        parties.act(new PartyActionPacket(TADEU.id(), TADEU.name(), PartyAction.OPEN, List.of(), false));

        assertThat(lobbyNotices).extracting(PartyNotice::key).containsExactly("party-open-no-permission");
    }

    @Test
    void leavingTheNetworkPassesTheLeadershipOn() {
        formParty();
        lobbyNotices.clear();
        bedwarsNotices.clear();
        players.remove(TADEU);

        parties.disconnected(TADEU.id());

        assertThat(bedwarsNotices).extracting(PartyNotice::key).containsExactly("party-member-disconnected", "party-new-leader");
        assertThat(lastParty().leader()).isEqualTo(DOBW.id());
    }

    @Test
    void theTickRemovesMembersOfflineForTooLong() {
        formParty();
        players.remove(DOBW);
        parties.disconnected(DOBW.id());
        network.clock().advance(Duration.ofMinutes(3));

        parties.tick();

        assertThat(lobbyNotices).extracting(PartyNotice::key).contains("party-offline-kicked");
        assertThat(broadcasts).hasAtLeastOneElementOfType(PartyRemovedPacket.class);
    }

    @Test
    void teleportingAsksTheLeaderServerAndConnectsWhoItAccepts() {
        formParty();
        List<PartyGatherRequest> asked = new ArrayList<>();
        lobby.bus().subscribe(PartyGatherRequest.TYPE, context -> {
            asked.add(context.packet());
            context.reply(new PartyGatherResponse(context.packet().members()));
        });

        act(TADEU, PartyAction.TELEPORT);

        assertThat(asked).singleElement().satisfies(request -> {
            assertThat(request.leader()).isEqualTo(TADEU.id());
            assertThat(request.members()).containsExactly(DOBW.id());
        });
        assertThat(players.connected).containsExactly(Map.entry(DOBW.id(), "lobby-1"));
        assertThat(lobbyNotices).extracting(PartyNotice::key).contains("party-summoned");
    }

    @Test
    void membersRefusedByTheLeaderServerStayWhereTheyAre() {
        formParty();
        lobby.bus().subscribe(PartyGatherRequest.TYPE, context -> context.reply(new PartyGatherResponse(List.of())));

        act(TADEU, PartyAction.TELEPORT);

        assertThat(players.connected).isEmpty();
    }

    @Test
    void aStartingServerReceivesEveryParty() throws Exception {
        formParty();

        PartySyncResponse response = lobby.bus()
            .request(PacketTarget.proxy(), new PartySyncRequest("lobby-1"), PartySyncResponse.class, Duration.ofSeconds(1))
            .get(2, TimeUnit.SECONDS);

        assertThat(response.parties()).singleElement().satisfies(party -> assertThat(party.memberIds()).containsExactly(TADEU.id(), DOBW.id()));
    }

    @Test
    void theOnlinePlayersAreBroadcastForTabCompletion() {
        parties.publishOnlinePlayers();

        assertThat(broadcasts).contains(new OnlinePlayersPacket(List.of("tadeu", "tadeudobw")));
    }

    private void formParty() {
        act(TADEU, PartyAction.INVITE, "tadeudobw");
        act(DOBW, PartyAction.ACCEPT, "tadeu");
    }

    private PartySnapshot lastParty() {
        return broadcasts.stream().filter(PartyUpdatedPacket.class::isInstance).map(PartyUpdatedPacket.class::cast).map(PartyUpdatedPacket::party)
            .reduce((first, second) -> second).orElseThrow();
    }

    private void act(PartyPlayer actor, PartyAction action, String... arguments) {
        parties.act(PartyActionPacket.of(actor.id(), actor.name(), action, arguments));
    }

    private static final class FakeNetwork implements PartyNetwork {

        private final Map<UUID, PartyPlayer> online = new HashMap<>();
        private final Map<UUID, String> servers = new HashMap<>();
        private final List<Map.Entry<UUID, String>> connected = new CopyOnWriteArrayList<>();

        void put(PartyPlayer player, String server) {
            online.put(player.id(), player);
            servers.put(player.id(), server);
        }

        void remove(PartyPlayer player) {
            online.remove(player.id());
            servers.remove(player.id());
        }

        @Override
        public Optional<PartyPlayer> online(String name) {
            return online.values().stream().filter(player -> player.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))).findFirst();
        }

        @Override
        public Optional<String> serverOf(UUID player) {
            return Optional.ofNullable(servers.get(player));
        }

        @Override
        public void connect(UUID player, String serverId) {
            connected.add(Map.entry(player, serverId));
        }

        @Override
        public List<String> onlineNames() {
            return online.values().stream().map(PartyPlayer::name).sorted().toList();
        }
    }
}
