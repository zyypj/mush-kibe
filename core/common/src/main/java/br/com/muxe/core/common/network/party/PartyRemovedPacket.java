package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.UUID;

public record PartyRemovedPacket(UUID partyId) implements Packet {

    public static final PacketType<PartyRemovedPacket> TYPE = PacketType.of(
        "party.removed",
        PartyRemovedPacket.class,
        PacketCodec.of((packet, out) -> out.writeUuid(packet.partyId()), in -> new PartyRemovedPacket(in.readUuid())),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(30))
    );
}
