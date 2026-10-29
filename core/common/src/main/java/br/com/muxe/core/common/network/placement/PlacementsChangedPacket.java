package br.com.muxe.core.common.network.placement;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.util.Objects;

public record PlacementsChangedPacket(String group) implements Packet {

    public static final PacketType<PlacementsChangedPacket> TYPE = PacketType.of(
        "placement.changed",
        PlacementsChangedPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeString(packet.group()),
            in -> new PlacementsChangedPacket(in.readString())
        )
    );

    public PlacementsChangedPacket {
        Objects.requireNonNull(group, "group");
    }
}
