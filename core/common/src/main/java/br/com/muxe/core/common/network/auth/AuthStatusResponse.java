package br.com.muxe.core.common.network.auth;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.UUID;

public record AuthStatusResponse(UUID playerId, boolean authenticated) implements Packet {

    public static final PacketType<AuthStatusResponse> TYPE = PacketType.of(
        "auth.status_response",
        AuthStatusResponse.class,
        PacketCodec.of(
            (packet, out) -> out.writeUuid(packet.playerId()).writeBoolean(packet.authenticated()),
            in -> new AuthStatusResponse(in.readUuid(), in.readBoolean())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );
}
