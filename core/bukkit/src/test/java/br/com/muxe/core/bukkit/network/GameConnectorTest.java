package br.com.muxe.core.bukkit.network;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.menu.button.MenuFixtures;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.testing.TestNetwork;
import org.junit.jupiter.api.Test;

class GameConnectorTest {

    private final TestNetwork network = new TestNetwork();
    private final ServerDirectory servers = MenuFixtures.servers();
    private final GameConnector connector = new GameConnector(
        servers,
        new NetworkGateway(network.node("lobby-1", "lobby", ServerKind.LOBBY).start().bus()),
        Runnable::run,
        MenuFixtures.MESSAGES);

    @Test
    void joiningALiveGameSendsNoChatMessage() {
        MenuFixtures.online(servers, "bedwars-1", "bedwars", 3);
        FakeSender player = FakeSender.named("Ana");

        connector.connect(player.player(), "bedwars", "BED WARS");

        assertThat(player.messages()).isEmpty();
    }

    @Test
    void aGameWithoutLiveServersStillSaysSo() {
        FakeSender player = FakeSender.named("Ana");

        connector.connect(player.player(), "bedwars", "BED WARS");

        assertThat(player.messages()).containsExactly(MenuFixtures.MESSAGES.get("game-unavailable", "name", "BED WARS"));
    }
}
