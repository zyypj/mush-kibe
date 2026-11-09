package br.com.muxe.core.common.network.player;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.UUID;

public record PlayerDisplayPacket(UUID playerId, String coloredName) implements Packet {

    public static final PacketType<PlayerDisplayPacket> TYPE = PacketType.of(
        "player.display",
        PlayerDisplayPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeUuid(packet.playerId()).writeString(packet.coloredName()),
            in -> new PlayerDisplayPacket(in.readUuid(), in.readString())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(30))
    );
}
