package br.com.muxe.auth.flow;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.auth.AuthStatusRequest;
import br.com.muxe.core.common.network.auth.AuthStatusResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProxyAuthenticationCheck {

    private final PacketBus packets;
    private final Duration timeout;

    public ProxyAuthenticationCheck(PacketBus packets, Duration timeout) {
        this.packets = packets;
        this.timeout = timeout;
    }

    public CompletableFuture<Boolean> isAuthenticated(UUID playerId) {
        return packets.request(PacketTarget.proxy(), new AuthStatusRequest(playerId), AuthStatusResponse.class, timeout)
            .thenApply(response -> response.playerId().equals(playerId) && response.authenticated())
            .exceptionally(error -> false);
    }
}
