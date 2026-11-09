package br.com.muxe.core.common.network.customroom;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CustomRoomCatalogPacket(String serverId, String game, List<String> schema, List<Mode> modes, List<Arena> arenas) implements Packet {

    public static final int CHUNK_CHARACTERS = 8_000;

    public static final PacketType<CustomRoomCatalogPacket> TYPE = PacketType.of(
        "custom_room.catalog",
        CustomRoomCatalogPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeString(packet.serverId()).writeString(packet.game())
                .writeList(packet.schema(), PacketWriter::writeString)
                .writeList(packet.modes(), Mode::write)
                .writeList(packet.arenas(), Arena::write),
            in -> new CustomRoomCatalogPacket(in.readString(), in.readString(), in.readList(PacketReader::readString), in.readList(Mode::read),
                in.readList(Arena::read))
        ),
        DeliveryPolicy.volatileState(Duration.ofSeconds(30))
    );

    public CustomRoomCatalogPacket {
        schema = List.copyOf(schema);
        modes = List.copyOf(modes);
        arenas = List.copyOf(arenas);
    }

    public static List<String> chunks(String text) {
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start += CHUNK_CHARACTERS) {
            chunks.add(text.substring(start, Math.min(text.length(), start + CHUNK_CHARACTERS)));
        }
        return chunks;
    }

    public String schemaText() {
        return String.join("", schema);
    }

    public Optional<Mode> mode(String id) {
        return modes.stream().filter(mode -> mode.id().equals(id)).findFirst();
    }

    public List<Arena> arenasOf(String mode) {
        return arenas.stream().filter(arena -> arena.mode().equals(mode)).toList();
    }

    public record Mode(String id, String displayName, int teamSize, int maxPlayers) {

        public int teams() {
            return Math.max(1, maxPlayers / Math.max(1, teamSize));
        }

        static void write(PacketWriter out, Mode mode) {
            out.writeString(mode.id()).writeString(mode.displayName()).writeVarInt(mode.teamSize()).writeVarInt(mode.maxPlayers());
        }

        static Mode read(PacketReader in) {
            return new Mode(in.readString(), in.readString(), in.readVarInt(), in.readVarInt());
        }
    }

    public record Arena(String mode, String name, String displayName) {

        static void write(PacketWriter out, Arena arena) {
            out.writeString(arena.mode()).writeString(arena.name()).writeString(arena.displayName());
        }

        static Arena read(PacketReader in) {
            return new Arena(in.readString(), in.readString(), in.readString());
        }
    }
}
