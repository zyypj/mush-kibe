package br.com.muxe.core.common.network.player;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;

public record PlayerLocateResponse(boolean online, String username, String serverId) implements Packet {

    public static final PacketType<PlayerLocateResponse> TYPE = PacketType.of(
        "player.locate_response",
        PlayerLocateResponse.class,
        PacketCodec.of(
            (packet, out) -> out
                .writeBoolean(packet.online())
                .writeString(packet.username())
                .writeNullableString(packet.serverId()),
            in -> new PlayerLocateResponse(in.readBoolean(), in.readString(), in.readNullableString())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );

    public static PlayerLocateResponse offline(String username) {
        return new PlayerLocateResponse(false, username, null);
    }
}
