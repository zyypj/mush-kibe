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

public record PartyGatherRequest(UUID leader, List<UUID> members) implements Packet {

    public static final PacketType<PartyGatherRequest> TYPE = PacketType.of(
        "party.gather_request",
        PartyGatherRequest.class,
        PacketCodec.of(
            (packet, out) -> out.writeUuid(packet.leader()).writeList(packet.members(), PacketWriter::writeUuid),
            in -> new PartyGatherRequest(in.readUuid(), in.readList(PacketReader::readUuid))
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );

    public PartyGatherRequest {
        members = List.copyOf(members);
    }
}
