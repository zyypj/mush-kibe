package br.com.muxe.core.common.messaging.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.server.ServerKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PacketCodecTest {

    @Test
    void readsBackEveryWrittenType() {
        UUID id = UUID.randomUUID();
        byte[] bytes = new PacketWriter()
            .writeByte(200)
            .writeBoolean(true)
            .writeShort(0x4D58)
            .writeInt(-42)
            .writeLong(Long.MAX_VALUE)
            .writeVarInt(300_000)
            .writeString("Olá, Muxe")
            .writeNullableString(null)
            .writeUuid(id)
            .writeNullableUuid(null)
            .writeBytes(new byte[] {1, 2, 3})
            .writeEnum(ServerKind.LOBBY)
            .writeList(List.of("a", "b"), PacketWriter::writeString)
            .toByteArray();

        PacketReader in = new PacketReader(bytes);

        assertThat(in.readByte()).isEqualTo(200);
        assertThat(in.readBoolean()).isTrue();
        assertThat(in.readShort()).isEqualTo(0x4D58);
        assertThat(in.readInt()).isEqualTo(-42);
        assertThat(in.readLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(in.readVarInt()).isEqualTo(300_000);
        assertThat(in.readString()).isEqualTo("Olá, Muxe");
        assertThat(in.readNullableString()).isNull();
        assertThat(in.readUuid()).isEqualTo(id);
        assertThat(in.readNullableUuid()).isNull();
        assertThat(in.readBytes()).containsExactly(1, 2, 3);
        assertThat(in.readEnum(ServerKind.class)).isEqualTo(ServerKind.LOBBY);
        assertThat(in.readList(PacketReader::readString)).containsExactly("a", "b");
        in.ensureFullyRead();
    }

    @Test
    void negativeVarIntsRoundTrip() {
        byte[] bytes = new PacketWriter().writeVarInt(-1).toByteArray();

        assertThat(new PacketReader(bytes).readVarInt()).isEqualTo(-1);
    }

    @Test
    void rejectsTruncatedInput() {
        byte[] bytes = new PacketWriter().writeLong(1).toByteArray();

        assertThatThrownBy(() -> new PacketReader(bytes, 0, 4).readLong())
            .isInstanceOf(PacketCodecException.class)
            .hasMessageContaining("truncated");
    }

    @Test
    void rejectsLeftoverBytes() {
        PacketReader in = new PacketReader(new PacketWriter().writeInt(1).writeInt(2).toByteArray());
        in.readInt();

        assertThatThrownBy(in::ensureFullyRead).isInstanceOf(PacketCodecException.class);
    }

    @Test
    void rejectsUnknownEnumConstants() {
        byte[] bytes = new PacketWriter().writeString("SPACESHIP").toByteArray();

        assertThatThrownBy(() -> new PacketReader(bytes).readEnum(ServerKind.class))
            .isInstanceOf(PacketCodecException.class)
            .hasMessageContaining("SPACESHIP");
    }

    @Test
    void rejectsOversizedStrings() {
        String huge = "x".repeat(PacketWriter.MAX_STRING_BYTES + 1);

        assertThatThrownBy(() -> new PacketWriter().writeString(huge)).isInstanceOf(PacketCodecException.class);
    }
}
