package br.com.muxe.core.common.network.auth;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.UUID;

public record PlayerAuthenticatedPacket(UUID playerId, String username, AuthenticationMethod method) implements Packet {

    public static final PacketType<PlayerAuthenticatedPacket> TYPE = PacketType.of(
        "auth.authenticated",
        PlayerAuthenticatedPacket.class,
        PacketCodec.of(
            (packet, out) -> out
                .writeUuid(packet.playerId())
                .writeString(packet.username())
                .writeEnum(packet.method()),
            in -> new PlayerAuthenticatedPacket(in.readUuid(), in.readString(), in.readEnum(AuthenticationMethod.class))
        ),
        DeliveryPolicy.durable(Duration.ofMinutes(5)).withMaxAge(Duration.ofSeconds(15))
    );
}
