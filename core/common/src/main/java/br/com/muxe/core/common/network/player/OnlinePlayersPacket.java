package br.com.muxe.core.common.network.player;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.List;

public record OnlinePlayersPacket(List<String> names) implements Packet {

    public static final PacketType<OnlinePlayersPacket> TYPE = PacketType.of(
        "network.players",
        OnlinePlayersPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeList(packet.names(), PacketWriter::writeString),
            in -> new OnlinePlayersPacket(in.readList(PacketReader::readString))
        ),
        DeliveryPolicy.volatileState(Duration.ofSeconds(10))
    );

    public OnlinePlayersPacket {
        names = List.copyOf(names);
    }
}
