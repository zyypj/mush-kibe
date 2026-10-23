package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.proxy.auth.AccessPolicy;
import br.com.muxe.core.proxy.auth.AuthenticationGate;
import br.com.muxe.core.proxy.config.RoutingSettings;
import br.com.muxe.core.proxy.routing.ServerRouter;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;

public final class ConnectionListener {

    private final AuthenticationGate gate;
    private final AccessPolicy access;
    private final ServerRouter router;
    private final RoutingSettings routing;
    private final ProxyMessages messages;

    public ConnectionListener(AuthenticationGate gate, AccessPolicy access, ServerRouter router, RoutingSettings routing, ProxyMessages messages) {
        this.gate = gate;
        this.access = access;
        this.router = router;
        this.routing = routing;
        this.messages = messages;
    }

    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        boolean authenticated = gate.isAuthenticated(event.getPlayer().getUniqueId());
        String group = authenticated ? routing.lobbyGroup() : routing.authGroup();
        Optional<RegisteredServer> initial = router.pick(group);
        if (initial.isPresent()) {
            event.setInitialServer(initial.get());
        } else {
            event.getPlayer().disconnect(messages.get(authenticated ? "no-lobby-server" : "no-auth-server"));
        }
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        String target = event.getOriginalServer().getServerInfo().getName();
        if (!access.mayConnect(player.getUniqueId(), target)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(messages.get("must-authenticate"));
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        gate.forget(event.getPlayer().getUniqueId());
    }
}
