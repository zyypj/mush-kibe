package br.com.muxe.core.common.network.player;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;

public record PlayerLocateRequest(String username) implements Packet {

    public static final PacketType<PlayerLocateRequest> TYPE = PacketType.of(
        "player.locate_request",
        PlayerLocateRequest.class,
        PacketCodec.of(
            (packet, out) -> out.writeString(packet.username()),
            in -> new PlayerLocateRequest(in.readString())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );
}
