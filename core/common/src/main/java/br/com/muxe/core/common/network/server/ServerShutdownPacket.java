package br.com.muxe.core.common.network.server;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;

public record ServerShutdownPacket(String serverId) implements Packet {

    public static final PacketType<ServerShutdownPacket> TYPE = PacketType.of(
        "server.shutdown",
        ServerShutdownPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeString(packet.serverId()),
            in -> new ServerShutdownPacket(in.readString())
        )
    );
}
