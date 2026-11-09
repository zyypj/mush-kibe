package br.com.muxe.minigame.api.network;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.Packet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record JoinRoomRequest(UUID playerId, String mode, String arena, String roomCode, List<UUID> companions) implements Packet {

    public static final int VERSION = 2;

    public static final PacketCodec<JoinRoomRequest> CODEC = PacketCodec.of(
        (packet, out) -> out.writeUuid(packet.playerId()).writeString(packet.mode()).writeString(packet.arena()).writeString(packet.roomCode())
            .writeList(packet.companions(), PacketWriter::writeUuid),
        in -> new JoinRoomRequest(in.readUuid(), in.readString(), in.readString(), in.readString(), in.readList(PacketReader::readUuid))
    );

    public JoinRoomRequest {
        companions = List.copyOf(companions);
    }

    public JoinRoomRequest(UUID playerId, String mode, String arena, String roomCode) {
        this(playerId, mode, arena, roomCode, List.of());
    }

    public Optional<String> wantedArena() {
        return arena.isBlank() ? Optional.empty() : Optional.of(arena);
    }

    public Optional<String> wantedRoom() {
        return roomCode.isBlank() ? Optional.empty() : Optional.of(roomCode);
    }

    public int seats() {
        return 1 + companions.size();
    }
}
