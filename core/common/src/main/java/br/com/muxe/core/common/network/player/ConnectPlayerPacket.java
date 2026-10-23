package br.com.muxe.core.common.network.player;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.util.UUID;

public record ConnectPlayerPacket(UUID playerId, DestinationType type, String destination) implements Packet {

    public static final PacketType<ConnectPlayerPacket> TYPE = PacketType.of(
        "player.connect",
        ConnectPlayerPacket.class,
        PacketCodec.of(
            (packet, out) -> out
                .writeUuid(packet.playerId())
                .writeEnum(packet.type())
                .writeString(packet.destination()),
            in -> new ConnectPlayerPacket(in.readUuid(), in.readEnum(DestinationType.class), in.readString())
        )
    );

    public static ConnectPlayerPacket toGroup(UUID playerId, String group) {
        return new ConnectPlayerPacket(playerId, DestinationType.GROUP, group);
    }

    public static ConnectPlayerPacket toServer(UUID playerId, String serverId) {
        return new ConnectPlayerPacket(playerId, DestinationType.SERVER, serverId);
    }
}
