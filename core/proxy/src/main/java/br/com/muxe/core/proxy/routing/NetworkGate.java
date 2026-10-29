package br.com.muxe.core.proxy.routing;

import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.proxy.config.RoutingSettings;

public final class NetworkGate {

    private final ServerDirectory directory;
    private final RoutingSettings routing;

    public NetworkGate(ServerDirectory directory, RoutingSettings routing) {
        this.directory = directory;
        this.routing = routing;
    }

    public boolean acceptsLogins() {
        return !directory.aliveInGroup(routing.authGroup()).isEmpty();
    }
}
