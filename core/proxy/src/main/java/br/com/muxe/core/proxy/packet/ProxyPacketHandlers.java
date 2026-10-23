package br.com.muxe.core.proxy.packet;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.PacketContext;
import br.com.muxe.core.common.network.auth.PlayerAuthenticatedPacket;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.network.player.DestinationType;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.proxy.auth.AccessPolicy;
import br.com.muxe.core.proxy.auth.AuthenticationGate;
import br.com.muxe.core.proxy.config.RoutingSettings;
import br.com.muxe.core.proxy.routing.ServerRouter;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.Optional;
import java.util.logging.Logger;

public final class ProxyPacketHandlers {

    private final ProxyServer proxy;
    private final AuthenticationGate gate;
    private final AccessPolicy access;
    private final ServerRouter router;
    private final RoutingSettings routing;
    private final ProxyMessages messages;
    private final Logger logger;

    public ProxyPacketHandlers(ProxyServer proxy, AuthenticationGate gate, AccessPolicy access, ServerRouter router, RoutingSettings routing, ProxyMessages messages, Logger logger) {
        this.proxy = proxy;
        this.gate = gate;
        this.access = access;
        this.router = router;
        this.routing = routing;
        this.messages = messages;
        this.logger = logger;
    }

    public void register(PacketBus bus) {
        bus.subscribe(PlayerAuthenticatedPacket.TYPE, this::onAuthenticated);
        bus.subscribe(ConnectPlayerPacket.TYPE, this::onConnect);
        bus.subscribe(PlayerLocateRequest.TYPE, this::onLocate);
    }

    private void onAuthenticated(PacketContext<PlayerAuthenticatedPacket> context) {
        PlayerAuthenticatedPacket packet = context.packet();
        String origin = context.metadata().origin();
        if (!access.isTrustedAuthOrigin(origin)) {
            logger.warning("Ignoring authentication of " + packet.username() + " sent by non-auth server " + origin);
            return;
        }
        Optional<Player> player = proxy.getPlayer(packet.playerId());
        if (player.isEmpty()) {
            return;
        }
        gate.authenticate(packet.playerId());
        if (!router.connect(player.get(), DestinationType.GROUP, routing.lobbyGroup())) {
            player.get().sendMessage(messages.get("no-lobby-server"));
        }
    }

    private void onConnect(PacketContext<ConnectPlayerPacket> context) {
        ConnectPlayerPacket packet = context.packet();
        Optional<Player> player = proxy.getPlayer(packet.playerId());
        if (player.isEmpty()) {
            return;
        }
        if (!gate.isAuthenticated(packet.playerId())) {
            player.get().sendMessage(messages.get("must-authenticate"));
            return;
        }
        if (!router.connect(player.get(), packet.type(), packet.destination())) {
            player.get().sendMessage(messages.get("no-server", "destination", packet.destination()));
        }
    }

    private void onLocate(PacketContext<PlayerLocateRequest> context) {
        String username = context.packet().username();
        PlayerLocateResponse response = proxy.getPlayer(username)
            .map(player -> new PlayerLocateResponse(
                true,
                player.getUsername(),
                player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null)))
            .orElseGet(() -> PlayerLocateResponse.offline(username));
        context.reply(response);
    }
}
