package br.com.muxe.core.bukkit.menu.button;

import br.com.muxe.core.bukkit.network.GameConnector;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.shop.ShopAccess;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

public final class MenuFixtures {

    public static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    public static final MessageCatalog MESSAGES = MessageCatalog.from(ShopFixtures.CONFIG.getConfigurationSection("messages"));

    private MenuFixtures() {
    }

    public static ServerDirectory servers() {
        return new ServerDirectory(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30));
    }

    public static void online(ServerDirectory servers, String id, String group, int players) {
        servers.update(new ServerStatus(new ServerIdentity(id, group, ServerKind.MINIGAME), players, 100, true, NOW, NOW));
    }

    public static ButtonActions actions(TestNetwork network, ServerDirectory servers) {
        NetworkGateway gateway = new NetworkGateway(network.node("lobby-1", "lobby", ServerKind.LOBBY).start().bus());
        return new ButtonActions(new GameConnector(servers, gateway, Runnable::run, MESSAGES), new ShopAccess(Optional.empty(), MESSAGES));
    }
}
