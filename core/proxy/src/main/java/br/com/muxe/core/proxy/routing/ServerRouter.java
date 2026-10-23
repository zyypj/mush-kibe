package br.com.muxe.core.proxy.routing;

import br.com.muxe.core.common.network.player.DestinationType;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;

public final class ServerRouter {

    private final ProxyServer proxy;
    private final ServerDirectory directory;

    public ServerRouter(ProxyServer proxy, ServerDirectory directory) {
        this.proxy = proxy;
        this.directory = directory;
    }

    public Optional<RegisteredServer> pick(String group) {
        return directory.leastLoaded(group).flatMap(status -> proxy.getServer(status.identity().id()));
    }

    public Optional<RegisteredServer> resolve(DestinationType type, String destination) {
        return switch (type) {
            case SERVER -> directory.find(destination).flatMap(status -> proxy.getServer(destination));
            case GROUP -> pick(destination);
        };
    }

    public boolean connect(Player player, DestinationType type, String destination) {
        Optional<RegisteredServer> server = resolve(type, destination);
        server.ifPresent(target -> player.createConnectionRequest(target).fireAndForget());
        return server.isPresent();
    }
}
