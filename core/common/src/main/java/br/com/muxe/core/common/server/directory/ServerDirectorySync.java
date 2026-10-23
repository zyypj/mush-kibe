package br.com.muxe.core.common.server.directory;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.PacketContext;
import br.com.muxe.core.common.network.server.ServerShutdownPacket;
import br.com.muxe.core.common.network.server.ServerStatusPacket;
import br.com.muxe.core.common.server.ServerIdentity;

public final class ServerDirectorySync {

    private final ServerDirectory directory;

    private ServerDirectorySync(ServerDirectory directory) {
        this.directory = directory;
    }

    public static void bind(PacketBus bus, ServerDirectory directory) {
        ServerDirectorySync sync = new ServerDirectorySync(directory);
        bus.subscribe(ServerStatusPacket.TYPE, sync::onStatus);
        bus.subscribe(ServerShutdownPacket.TYPE, sync::onShutdown);
    }

    private void onStatus(PacketContext<ServerStatusPacket> context) {
        ServerStatusPacket packet = context.packet();
        directory.update(new ServerStatus(
            new ServerIdentity(packet.serverId(), packet.group(), packet.kind()),
            packet.onlinePlayers(),
            packet.maxPlayers(),
            packet.joinable(),
            context.metadata().sentAt(),
            context.metadata().receivedAt()
        ));
    }

    private void onShutdown(PacketContext<ServerShutdownPacket> context) {
        directory.remove(context.packet().serverId());
    }
}
