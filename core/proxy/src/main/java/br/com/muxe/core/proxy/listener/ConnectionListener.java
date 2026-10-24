package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.proxy.auth.AccessPolicy;
import br.com.muxe.core.proxy.auth.AuthenticationGate;
import br.com.muxe.core.proxy.config.RoutingSettings;
import br.com.muxe.core.proxy.routing.InitialRoute;
import br.com.muxe.core.proxy.routing.InitialRoutes;
import br.com.muxe.core.proxy.routing.ServerRouter;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionListener {

    private final AuthenticationGate gate;
    private final AccessPolicy access;
    private final ServerRouter router;
    private final RoutingSettings routing;
    private final ProxyMessages messages;
    private final Set<UUID> waitingForLobby = ConcurrentHashMap.newKeySet();

    public ConnectionListener(AuthenticationGate gate, AccessPolicy access, ServerRouter router, RoutingSettings routing, ProxyMessages messages) {
        this.gate = gate;
        this.access = access;
        this.router = router;
        this.routing = routing;
        this.messages = messages;
    }

    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        InitialRoute<RegisteredServer> route = InitialRoutes.choose(gate.isAuthenticated(player.getUniqueId()), routing, router::pick);
        switch (route) {
            case InitialRoute.Join<RegisteredServer> join -> {
                event.setInitialServer(join.server());
                if (join.lobbyUnavailable()) {
                    waitingForLobby.add(player.getUniqueId());
                }
            }
            case InitialRoute.Refuse<RegisteredServer> refuse -> player.disconnect(messages.get(refuse.messageKey()));
        }
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        if (waitingForLobby.remove(player.getUniqueId())) {
            player.sendMessage(messages.get("no-lobby-server"));
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
        waitingForLobby.remove(event.getPlayer().getUniqueId());
        gate.forget(event.getPlayer().getUniqueId());
    }
}
