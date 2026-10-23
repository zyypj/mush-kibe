package br.com.muxe.core.common.network.server;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.server.ServerKind;
import java.time.Duration;

public record ServerStatusPacket(
    String serverId,
    String group,
    ServerKind kind,
    int onlinePlayers,
    int maxPlayers,
    boolean joinable
) implements Packet {

    public static final PacketType<ServerStatusPacket> TYPE = PacketType.of(
        "server.status",
        ServerStatusPacket.class,
        PacketCodec.of(
            (packet, out) -> out
                .writeString(packet.serverId())
                .writeString(packet.group())
                .writeEnum(packet.kind())
                .writeVarInt(packet.onlinePlayers())
                .writeVarInt(packet.maxPlayers())
                .writeBoolean(packet.joinable()),
            in -> new ServerStatusPacket(
                in.readString(),
                in.readString(),
                in.readEnum(ServerKind.class),
                in.readVarInt(),
                in.readVarInt(),
                in.readBoolean()
            )
        ),
        DeliveryPolicy.volatileState(Duration.ofSeconds(10))
    );
}
