package br.com.muxe.core.common.network.auth;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.UUID;

public record AuthStatusRequest(UUID playerId) implements Packet {

    public static final PacketType<AuthStatusRequest> TYPE = PacketType.of(
        "auth.status_request",
        AuthStatusRequest.class,
        PacketCodec.of(
            (packet, out) -> out.writeUuid(packet.playerId()),
            in -> new AuthStatusRequest(in.readUuid())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );
}
