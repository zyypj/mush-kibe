package br.com.muxe.core.bukkit.network;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.menu.button.MenuFixtures;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.testing.TestNetwork;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameConnectorTest {

    private final TestNetwork network = new TestNetwork();
    private final ServerDirectory servers = MenuFixtures.servers();
    private final List<ConnectPlayerPacket> proxyReceived = new CopyOnWriteArrayList<>();
    private GameConnector connector;

    @BeforeEach
    void setUp() {
        TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> proxyReceived.add(context.packet()));
        proxy.start();
        connector = new GameConnector(
            servers,
            new NetworkGateway(network.node("lobby-1", "lobby", ServerKind.LOBBY).start().bus()),
            Runnable::run,
            MenuFixtures.MESSAGES);
    }

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void joiningALiveGameSendsNoChatMessage() {
        MenuFixtures.online(servers, "bedwars-1", "bedwars", 3);
        FakeSender player = FakeSender.named("Ana");

        connector.connect(player.player(), "bedwars", "BED WARS");

        assertThat(player.messages()).isEmpty();
    }

    @Test
    void joiningAGameGoesToItsLobbyAndCountsEveryone() {
        MenuFixtures.online(servers, "bedwars-1", "bedwars", 3);
        MenuFixtures.online(servers, "bedwars-lobby-1", "bedwars-lobby", 2);
        FakeSender player = FakeSender.named("Ana");

        connector.connect(player.player(), "bedwars", "BED WARS");

        assertThat(proxyReceived).containsExactly(ConnectPlayerPacket.toGroup(player.player().getUniqueId(), "bedwars-lobby"));
        assertThat(connector.players("bedwars")).isEqualTo(5);
    }

    @Test
    void aGameWithoutALobbyIsJoinedDirectly() {
        MenuFixtures.online(servers, "skywars-1", "skywars", 1);
        FakeSender player = FakeSender.named("Ana");

        connector.connect(player.player(), "skywars", "SKY WARS");

        assertThat(proxyReceived).containsExactly(ConnectPlayerPacket.toGroup(player.player().getUniqueId(), "skywars"));
    }

    @Test
    void aGameWithoutLiveServersStillSaysSo() {
        FakeSender player = FakeSender.named("Ana");

        connector.connect(player.player(), "bedwars", "BED WARS");

        assertThat(player.messages()).containsExactly(MenuFixtures.MESSAGES.get("game-unavailable", "name", "BED WARS"));
        assertThat(proxyReceived).isEmpty();
    }
}
