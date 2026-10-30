package br.com.muxe.minigame.engine.world.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.world.nbt.NbtTag.ByteArrayTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ByteTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.DoubleTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.FloatTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntArrayTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ListTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.LongTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ShortTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.StringTag;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NbtIoTest {

    @Test
    void everyTagTypeSurvivesARoundTrip() {
        Map<String, NbtTag> values = new LinkedHashMap<>();
        values.put("byte", new ByteTag((byte) -3));
        values.put("short", new ShortTag((short) 300));
        values.put("int", new IntTag(70_000));
        values.put("long", new LongTag(1L << 40));
        values.put("float", new FloatTag(1.5f));
        values.put("double", new DoubleTag(-2.25));
        values.put("bytes", new ByteArrayTag(new byte[] {1, 2, 3}));
        values.put("text", new StringTag("Cama do Azul"));
        values.put("list", new ListTag(NbtTag.INT, List.of(new IntTag(1), new IntTag(2))));
        values.put("ints", new IntArrayTag(new int[] {7, 8}));
        values.put("child", new CompoundTag(Map.of("id", new StringTag("Sign"))));
        CompoundTag root = new CompoundTag(values);

        CompoundTag read = NbtIo.fromBytes(NbtIo.toBytes(root));

        assertThat(read.values().keySet()).containsExactlyElementsOf(values.keySet());
        assertThat(read.intValue("byte", 0)).isEqualTo(-3);
        assertThat(read.intValue("short", 0)).isEqualTo(300);
        assertThat(read.intValue("int", 0)).isEqualTo(70_000);
        assertThat(read.get("long")).contains(new LongTag(1L << 40));
        assertThat(read.get("float")).contains(new FloatTag(1.5f));
        assertThat(read.get("double")).contains(new DoubleTag(-2.25));
        assertThat(read.bytes("bytes")).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(1, 2, 3));
        assertThat(read.get("text")).contains(new StringTag("Cama do Azul"));
        assertThat(read.list("list")).containsExactly(new IntTag(1), new IntTag(2));
        assertThat(read.ints("ints")).hasValueSatisfying(ints -> assertThat(ints).containsExactly(7, 8));
        assertThat(read.compound("child").orElseThrow().get("id")).contains(new StringTag("Sign"));
    }

    @Test
    void anEmptyListIsWrittenWithTheEndType() {
        CompoundTag root = new CompoundTag(Map.of("empty", new ListTag(NbtTag.COMPOUND, List.of())));

        byte[] bytes = NbtIo.toBytes(root);

        assertThat(NbtIo.fromBytes(bytes).list("empty")).isEmpty();
        int rootHeader = 3;
        int childHeader = 1 + 2 + "empty".length();
        assertThat(bytes[rootHeader + childHeader]).isEqualTo(NbtTag.END);
    }

    @Test
    void theRootMustBeACompound() {
        assertThatThrownBy(() -> NbtIo.fromBytes(new byte[] {NbtTag.INT, 0, 0, 0, 0, 0, 1}))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessageContaining("compound");
    }

    @Test
    void negativeLengthsAreRejectedInsteadOfAllocated() {
        byte[] bytes = {NbtTag.COMPOUND, 0, 0, NbtTag.BYTE_ARRAY, 0, 1, 'a', (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        assertThatThrownBy(() -> NbtIo.fromBytes(bytes)).isInstanceOf(UncheckedIOException.class).hasMessageContaining("Negative");
    }
}
