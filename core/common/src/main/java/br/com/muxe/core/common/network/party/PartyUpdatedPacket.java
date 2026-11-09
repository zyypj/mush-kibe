package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.time.Duration;

public record PartyUpdatedPacket(PartySnapshot party) implements Packet {

    public static final PacketType<PartyUpdatedPacket> TYPE = PacketType.of(
        "party.updated",
        PartyUpdatedPacket.class,
        PacketCodec.of((packet, out) -> PartyCodecs.writeSnapshot(out, packet.party()), in -> new PartyUpdatedPacket(PartyCodecs.readSnapshot(in))),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(30))
    );
}
