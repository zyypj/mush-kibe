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

public record PartyActionPacket(UUID actor, String actorName, PartyAction action, List<String> arguments, boolean allowed) implements Packet {

    public static final PacketType<PartyActionPacket> TYPE = PacketType.of(
        "party.action",
        PartyActionPacket.class,
        PacketCodec.of(
            (packet, out) -> out
                .writeUuid(packet.actor())
                .writeString(packet.actorName())
                .writeEnum(packet.action())
                .writeList(packet.arguments(), PacketWriter::writeString)
                .writeBoolean(packet.allowed()),
            in -> new PartyActionPacket(in.readUuid(), in.readString(), in.readEnum(PartyAction.class), in.readList(PacketReader::readString), in.readBoolean())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(10))
    );

    public PartyActionPacket {
        arguments = List.copyOf(arguments);
    }

    public static PartyActionPacket of(UUID actor, String actorName, PartyAction action, String... arguments) {
        return new PartyActionPacket(actor, actorName, action, List.of(arguments), true);
    }

    public String argument() {
        return arguments.isEmpty() ? "" : arguments.getFirst();
    }
}
