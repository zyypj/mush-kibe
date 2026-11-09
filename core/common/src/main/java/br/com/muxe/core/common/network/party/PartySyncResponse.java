package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.time.Duration;
import java.util.List;

public record PartySyncResponse(List<PartySnapshot> parties) implements Packet {

    public static final PacketType<PartySyncResponse> TYPE = PacketType.of(
        "party.sync_response",
        PartySyncResponse.class,
        PacketCodec.of(
            (packet, out) -> out.writeList(packet.parties(), PartyCodecs::writeSnapshot),
            in -> new PartySyncResponse(in.readList(PartyCodecs::readSnapshot))
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );

    public PartySyncResponse {
        parties = List.copyOf(parties);
    }
}
