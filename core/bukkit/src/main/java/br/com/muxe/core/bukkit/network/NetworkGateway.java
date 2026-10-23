package br.com.muxe.core.bukkit.network;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class NetworkGateway {

    private final PacketBus packets;

    public NetworkGateway(PacketBus packets) {
        this.packets = packets;
    }

    public CompletableFuture<Void> connectToGroup(UUID playerId, String group) {
        return packets.publish(PacketTarget.proxy(), ConnectPlayerPacket.toGroup(playerId, group));
    }

    public CompletableFuture<Void> connectToServer(UUID playerId, String serverId) {
        return packets.publish(PacketTarget.proxy(), ConnectPlayerPacket.toServer(playerId, serverId));
    }
}
