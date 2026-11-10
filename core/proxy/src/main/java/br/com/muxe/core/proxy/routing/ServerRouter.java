package br.com.muxe.core.proxy.routing;

import br.com.muxe.core.common.network.player.DestinationType;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Comparator;
import java.util.Optional;

public final class ServerRouter {

    private final ProxyServer proxy;
    private final ServerDirectory directory;
    private final ProxyMessages messages;

    public ServerRouter(ProxyServer proxy, ServerDirectory directory, ProxyMessages messages) {
        this.proxy = proxy;
        this.directory = directory;
        this.messages = messages;
    }

    public Optional<RegisteredServer> pick(String group) {
        return directory.leastLoaded(group).flatMap(status -> proxy.getServer(status.identity().id()));
    }

    public Optional<RegisteredServer> pickExcept(String group, String excludedId) {
        return directory.aliveInGroup(group).stream()
            .filter(status -> !status.identity().id().equals(excludedId))
            .filter(ServerStatus::hasRoom)
            .min(Comparator.comparingInt(ServerStatus::onlinePlayers).thenComparing(status -> status.identity().id()))
            .flatMap(status -> proxy.getServer(status.identity().id()));
    }

    public boolean isAlive(String serverId) {
        return directory.find(serverId).isPresent();
    }

    public Optional<RegisteredServer> resolve(DestinationType type, String destination) {
        return switch (type) {
            case SERVER -> directory.find(destination).flatMap(status -> proxy.getServer(destination));
            case GROUP -> pick(destination);
        };
    }

    public boolean connect(Player player, DestinationType type, String destination) {
        return connect(player, type, destination, false);
    }

    public boolean join(Player player, String serverId) {
        return connect(player, DestinationType.SERVER, serverId, true);
    }

    private boolean connect(Player player, DestinationType type, String destination, boolean quietWhenThere) {
        Optional<RegisteredServer> server = resolve(type, destination);
        server.ifPresent(target -> player.createConnectionRequest(target).connect().whenComplete((result, error) -> {
            Optional<Status> status = error == null && result != null ? Optional.of(result.getStatus()) : Optional.empty();
            ConnectReplies.messageKey(status, quietWhenThere).ifPresent(key -> player.sendMessage(messages.get(player, key)));
        }));
        return server.isPresent();
    }
}
