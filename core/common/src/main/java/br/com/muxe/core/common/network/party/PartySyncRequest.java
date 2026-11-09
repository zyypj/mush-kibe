package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;

public record PartySyncRequest(String serverId) implements Packet {

    public static final PacketType<PartySyncRequest> TYPE = PacketType.of(
        "party.sync_request",
        PartySyncRequest.class,
        PacketCodec.of((packet, out) -> out.writeString(packet.serverId()), in -> new PartySyncRequest(in.readString())),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );
}
