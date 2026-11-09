package br.com.muxe.core.common.network.customroom;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateCustomRoomRequest(UUID leader, String leaderName, String game, String mode, List<String> maps, RoomOptions options,
                                      List<UUID> companions) implements Packet {

    public static final PacketType<CreateCustomRoomRequest> TYPE = PacketType.of(
        "custom_room.create_request",
        CreateCustomRoomRequest.class,
        PacketCodec.of(
            (packet, out) -> {
                out.writeUuid(packet.leader()).writeString(packet.leaderName()).writeString(packet.game()).writeString(packet.mode())
                    .writeList(packet.maps(), PacketWriter::writeString);
                RoomOptions.write(out, packet.options());
                out.writeList(packet.companions(), PacketWriter::writeUuid);
            },
            in -> new CreateCustomRoomRequest(in.readUuid(), in.readString(), in.readString(), in.readString(), in.readList(PacketReader::readString),
                RoomOptions.read(in), in.readList(PacketReader::readUuid))
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );

    public CreateCustomRoomRequest {
        Objects.requireNonNull(leader, "leader");
        Objects.requireNonNull(leaderName, "leaderName");
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(options, "options");
        maps = List.copyOf(maps);
        companions = List.copyOf(companions);
    }
}
