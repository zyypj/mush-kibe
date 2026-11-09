package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record PartyGatherResponse(List<UUID> accepted) implements Packet {

    public static final PacketType<PartyGatherResponse> TYPE = PacketType.of(
        "party.gather_response",
        PartyGatherResponse.class,
        PacketCodec.of(
            (packet, out) -> out.writeList(packet.accepted(), PacketWriter::writeUuid),
            in -> new PartyGatherResponse(in.readList(PacketReader::readUuid))
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );

    public PartyGatherResponse {
        accepted = List.copyOf(accepted);
    }
}
