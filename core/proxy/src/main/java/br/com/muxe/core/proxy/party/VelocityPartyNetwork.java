package br.com.muxe.core.proxy.party;

import br.com.muxe.core.common.network.player.DestinationType;
import br.com.muxe.core.common.party.domain.PartyPlayer;
import br.com.muxe.core.proxy.auth.AuthenticationGate;
import br.com.muxe.core.proxy.routing.ServerRouter;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class VelocityPartyNetwork implements PartyNetwork {

    private final ProxyServer proxy;
    private final AuthenticationGate gate;
    private final ServerRouter router;

    public VelocityPartyNetwork(ProxyServer proxy, AuthenticationGate gate, ServerRouter router) {
        this.proxy = proxy;
        this.gate = gate;
        this.router = router;
    }

    @Override
    public Optional<PartyPlayer> online(String name) {
        return proxy.getPlayer(name)
            .filter(player -> gate.isAuthenticated(player.getUniqueId()))
            .map(player -> new PartyPlayer(player.getUniqueId(), player.getUsername()));
    }

    @Override
    public Optional<String> serverOf(UUID player) {
        return proxy.getPlayer(player).flatMap(Player::getCurrentServer).map(ServerConnection::getServerInfo).map(info -> info.getName());
    }

    @Override
    public void connect(UUID player, String serverId) {
        proxy.getPlayer(player).ifPresent(found -> router.connect(found, DestinationType.SERVER, serverId));
    }

    @Override
    public List<String> onlineNames() {
        return proxy.getAllPlayers().stream()
            .filter(player -> gate.isAuthenticated(player.getUniqueId()))
            .map(Player::getUsername)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }
}
